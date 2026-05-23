! * * * * * REMAPMODEL * * * * *
!
! This program allows one to remap coordinates in a model, in two ways:
! 1) The set of Z values may be mapped, one-to-one, to any arbitrary
! new set of Z values; and
! 2) The X, Y or Z coordinates may be shifted by a constant.
!
! Also, a mapping can be set up easily for the case where serial section
! tomograms are being rejoined with different spacings.
!
! See man page for details.
!
! David Mastronarde  5/8/89
!
! $Id$
  !
program remapmodel
  use fortmodel
  implicit none
  integer LIMSEC, LIMINDEX, LIMCHUNK
  parameter (LIMSEC = 100000, LIMINDEX = 10 * LIMSEC, LIMCHUNK = 10000)
  integer*4 listZ(LIMSEC), newList(LIMSEC), indexZlist(LIMINDEX)
  integer*4 izStEndFrom(LIMCHUNK), izStEndTo(LIMCHUNK)
  integer*4, allocatable :: izList(:) 
  real*4 zNewList(LIMSEC), thicknesses(LIMSEC)
  !
  character*320 modelFile, newModel, thickFile
  character*1024 listString
  logical exist, readw_or_imod, doThickness
  integer*4 getImodMaxes, putImodMaxes, putImodZscale, getImodHeadO
  !
  integer*4 numInZlist, iobj, indObj, ipnt, nlistZ, i, numNew, numThick
  integer*4 ifReorder, numInObj, ibase, izVal, zNewVal, indMove, itemp, index, j
  integer*4 lastKeep, idir, idirFirst, inOrder, maxX, maxY, maxZ, minZ, maxIndex
  integer*4 ierr, numFrom, numTo, ifAdd, ifEntered, izBase, numInSec, ich, izOut
  integer*4 izStartOld, izEndOld, idirOld, idirNew, izStartNew, izEndNew, izChunk, iz
  real*4 xadd, yAdd, zAdd, cumThick, scaleThick, bigThickness, bigSecStart, bigSecEnd
  real*4 pixelSize, zScale
  logical b3dxor
  !
  logical pipInput
  integer*4 numOptArg, numNonOptArg
  integer*4 PipGetInteger, PipGetBoolean, PipGetFloat
  integer*4 PipGetString, PipNumberOfEntries
  integer*4 PipGetIntegerArray, PipGetThreeFloats
  integer*4 PipGetInOutFile
  !
  ! fallbacks from ../../manpages/autodoc2man -3 2  remapmodel
  !
  integer numOptions
  parameter (numOptions = 13)
  character*(40 * numOptions) options(1)
  options(1) = &
      'input:InputFile:FN:@output:OutputFile:FN:@old:OldZList:LI:@'// &
      'full:FullRangeInZ:B:@new:NewZList:LI:@add:AddToAllPoints:FT:@'// &
      'reorder:ReorderPointsInZ:I:@fromchunks:FromChunkLimits:IA:@'// &
      'tochunks:ToChunkLimits:IA:@thick:ThicknessFile:FN:@'// &
      'long:LongRangeThickness:FT:@pixel:PixelSizeOrScaling:F:@help:usage:B:'
  !
  ifReorder = 0
  xadd = 0.
  yAdd = 0.
  zAdd = 0.
  ifEntered = 0
  doThickness = .false.
  !
  ! Pip startup: set error, parse options, check help, set flag if used
  !
  call PipReadOrParseOptions(options, numOptions, 'remapmodel', &
      'ERROR: REMAPMODEL - ', .true., 3, 1, 1, numOptArg, &
      numNonOptArg)
  pipInput = numOptArg + numNonOptArg > 0
  !
  if (PipGetInOutFile('InputFile', 1, 'Name of input model file', &
      modelFile) .ne. 0) call exitError('No input file specified')
  if (PipGetInOutFile('InputFile', 2, 'Name of output model file', &
      newModel) .ne. 0) call exitError('No output file specified')
  !
  ! read in the model
  !
  exist = readw_or_imod(modelFile)
  if (.not.exist) call exitError('Reading model file')
  allocate(izList(max_pt), stat = ierr)
  call memoryError(ierr, 'array for point z values')
  !
  ! scale to index coordinates
  !
  call scale_model(0)
  !
  ! make list of all z values in the model and set default output list
  !
  numInZlist = 0
  do iobj = 1, max_mod_obj
    do indObj = 1, npt_in_obj(iobj)
      ipnt = abs(object(indObj + ibase_obj(iobj)))
      izVal = nint(p_coord(3, ipnt))
      ifAdd = 1
      i = 1
      do while (i <= numInZlist .and. ifAdd == 1)
        if (izList(i) == izVal) ifAdd = 0
        i = i + 1
      enddo
      if (ifAdd == 1) then
        numInZlist = numInZlist + 1
        if (numInZlist > LIMSEC) call exitError('Too many Z values for arrays')
        izList(numInZlist) = izVal
      endif
    enddo
  enddo
  call fill_listz(izList, numInZlist, listZ, nlistZ)
  minZ = listZ(1)
  maxIndex = listZ(nlistZ)
  do i = 1, nlistZ
    newList(i) = listZ(i)
  enddo
  numNew = nlistZ
  ierr = getImodMaxes(maxX, maxY, maxZ)
  !
  ! get options from pip input
  !
  if (pipInput) then
    ierr = PipGetInteger('ReorderPointsInZ', ifReorder)
    ifAdd = 1 - PipGetThreeFloats('AddToAllPoints', xadd, yAdd, zAdd)
    numFrom = 0
    numTo = 0
    ierr = PipGetIntegerArray('FromChunkLimits', izStEndFrom, numFrom, &
        LIMCHUNK)
    ierr = PipGetIntegerArray('ToChunkLimits', izStEndTo, numTo, LIMCHUNK)
    if (b3dxor(numFrom > 0, numTo > 0)) call exitError( &
        'You must enter both -fromchunks and -tochunks or neither')
    if (PipGetString('ThicknessFile', thickFile) == 0) then
      doThickness = .true.
      if (ifReorder .ne. 0 .or. numFrom > 0) call exiterror( &
          'You cannot endter -reorder or -tochunks with thicknesses')
      call dopen(3, thickFile, 'ro', 'f')
      numThick = 1
10    read(3, *, err = 20, end = 30)thicknesses(numThick)
      numThick = numThick + 1
      if (numThick > LIMSEC) call exitError('Too many thicknesses for arrays')
      go to 10
20    call exitError('Reading thickness file')
30    scaleThick = 1.

      ! Get possible parameters for scaling the values to long-range accuracy
      if (PipGetThreeFloats('LongRangeThickness', bigThickness, bigSecStart, bigSecEnd) &
          == 0) then
        if (bigSecStart < 0 .or. nint(bigSecEnd) >= numThick) call exitError( &
            'Starting or ending section for long range thickness out of range')
        if (nint(bigSecStart) <= nint(bigSecEnd)) call exitError('Starting section'// &
            ' number for long range thickness is larger than ending section')
        cumThick = 0.
        do i = 1, nint(bigSecStart) + 1, nint(bigSecEnd)
          cumThick = cumThick + thicknesses(i)
        enddo
        scaleThick = bigThickness / cumThick
      endif

      ! Get a pixel size one way or the other: the Z values need to be divided by that
      ! so incorporate it into the scaling
      ierr = getImodHeadO(pixelSize, zscale);
      if (ierr == 0) pixelSize = pixelSize * 1000.
      if (PipGetFloat('PixelSizeOrScaling', pixelSize) .ne. 0) then
        if (ierr .ne. 0) call exitError &
            ('A pixel size or scaling must be entered; the model has no pixel size')
      endif
      scaleThick = scaleThick / pixelSize

      cumThick = 0.
      do i = 1, numThick
        zNewList(i) = scaleThick * (cumThick + 0.5 * thicknesses(i))
        cumThick = cumThick + thicknesses(i)
      enddo

      if (maxIndex >= numThick) call exitError( &
          'There are not thicknesses in the file to correct all model Z values')
      maxZ = maxIndex + 1
    endif

    if (numFrom == 0) then
      !
      ! No chunks: get old list if entered, or full one
      !
      ierr = PipGetBoolean('FullRangeInZ', ifEntered)
      if (PipGetString('OldZList', listString) == 0) then
        if (doThickness) call exitError('You cannot enter an old list with thicknesses')
        if (ifEntered .ne. 0) call exitError( &
            &'You cannot enter both an old list AND -full')
        call parselist(listString, listZ, nlistZ)
        if (nlistZ > LIMSEC) call exitError( &
            'Too many Z values in new list for arrays')
        ifEntered = 1
        do i = 1, nlistZ - 1
          if (listZ(i) >= listZ(i + 1)) call exitError( &
              'Input list of Z values must be in increasing order')
        enddo
      else if (ifEntered .ne. 0 .or. doThickness) then
        if (maxZ > LIMSEC) call exitError('Model range in Z too big for arrays')
        do i = 1, maxZ
          listZ(i) = i - 1
        enddo
        nlistZ = maxZ
      endif
      !
      ! Make sure input list is still default for output
      !
      do i = 1, nlistZ
        newList(i) = listZ(i)
      enddo
      numNew = nlistZ
      !
      ! No chunks: get new list; it's required unless adding to coordinates
      !
      if (PipGetString('NewZList', listString) == 0) then
        if (doThickness) call exitError('You cannot enter a new Z list with thicknesses')
        call parselist(listString, newList, numNew)
        if (numNew > LIMSEC) call exitError('Too many Z values for arrays')
      else if (ifAdd == 0 .and. .not.doThickness) then
        call exitError( 'No list of new Z values specified')
      endif
    else
      !
      ! Chunks: build up in and out lists
      !
      if (PipGetBoolean('FullRangeInZ', ifEntered) + PipGetString( &
          'OldZList', listString) + PipGetString('NewZList', listString) &
          .ne. 3) call exitError('You cannot enter -new, -old, or -full '// &
          'with chunk entries')
      if (numFrom .ne. numTo) call exitError( &
          'Same number of values required with -tochunk and -fromchunk')
      nlistZ = 0
      izBase = 0
      do ich = 1, numFrom / 2
        izStartOld = izStEndFrom(2 * ich - 1)
        izEndOld = izStEndFrom(2 * ich)
        numInSec = abs(izStartOld -  izEndOld) + 1
        idirOld = 1
        idirNew = 1
        izStartNew = izStEndTo(2 * ich - 1)
        izEndNew = izStEndTo(2 * ich)
        if (izStartOld > izEndOld) idirOld = -1
        if (izStartNew > izEndNew) idirNew = -1
        if (nlistZ + numInSec > LIMSEC) call exitError( &
            'Chunk ranges make too many values for arrays')
        !
        ! For each z value in a chunk, get z value inside original section
        ! and figure out the fate of that z value in the new joined volume
        !
        do i = 0, numInSec - 1
          izChunk = izStartOld + i * idirOld
          iz = i + nlistZ
          listZ(iz + 1) = iz
          izOut = -999
          if (idirNew * (izChunk - izStartNew) >= 0 .and. &
              idirNew * (izChunk - izEndNew) <= 0) &
              izOut = izBase + (izChunk - izStartNew) / idirNew
          newList(iz + 1) = izOut
        enddo
        nlistZ = nlistZ + numInSec
        izBase = izBase + abs(izStartNew - izEndNew) + 1
      enddo
      numNew = nlistZ
      minZ = min(minZ, 0)
      ifEntered = 1
      print *,'Z values being mapped:'
      call wrlist(listZ, nlistZ)
      print *,'Values being mapped to:'
      call wrlist(newList, numNew)
    endif
  endif
  !
  if (ifEntered == 0 .and. .not.doThickness) then
    print *,'The current list of Z values (nearest integers)'// &
        ' in model is:'
    call wrlist(listZ, nlistZ)
  endif
  !
  ! get new list of z values for remapping
  !
  if (.not. pipInput) then
    write(*,'(/,a,i4,a,/,a,/,a,/,a)') ' Enter new list of', nlistZ, &
        ' Z values to remap these values to.', &
        ' Enter ranges just as above, or / to leave list alone,', &
        '   or -1 to replace each Z value with its negative.', &
        ' Use numbers from -999 to -990 to remove points with a'// &
        ' particular Z value.'
    call rdlist(5, newList, numNew)
    if (numNew > LIMSEC) call exitError('Too many Z values for arrays')
    !
    !
    write(*,'(1x,a,$)') &
        'Amounts to ADD to ALL X, Y and Z coordinates: '
    read(*,*) xadd, yAdd, zAdd
  endif

  if (numNew == 1 .and. newList(1) == -1) then
    do i = 1, nlistZ
      newList(i) = -listZ(i)
    enddo
  elseif (numNew .ne. nlistZ) then
    call exitError('Number of Z values does not correspond between old '// &
        'and new lists')
  endif
  if (.not. doThickness) then
    do i = 1, nlistZ
      zNewList(i) = newList(i)
    enddo
  endif
  !
  ! Build index list from all Z's that are being mapped
  ! Initialize list for range of anything in model or input z list
  !
  minZ = min(minZ, listZ(1))
  maxIndex = max(maxIndex, listZ(nlistZ))
  if (maxIndex - minZ + 1 > LIMINDEX) call exitError( &
      'Too many Z values for index list array')
  do i = minZ, maxIndex
    indexZlist(i + 1 - minZ) = 0
  enddo
  do i = 1, nlistZ
    indexZlist(listZ(i) + 1 - minZ) = i
  enddo
  !
  ! see if there are any Z's out of order: find direction between
  ! first retained Z's and make sure all other intervals match
  !
  inOrder = 1
  idirFirst = 0
  lastKeep = 0
  do i = 1, nlistZ
    if (zNewList(i) < -999 .or. zNewList(i) > -990) then
      !
      ! if this is a retained Z and there is a previous one
      ! get the sign of the interval
      !
      if (lastKeep > 0) then
        idir = sign(1., zNewList(i) - zNewList(lastKeep))
        !
        ! store the sign the first time, compare after that
        !
        if (idirFirst == 0) then
          idirFirst = idir
        elseif (idir .ne. idirFirst) then
          inOrder = 0
        endif
      endif
      lastKeep = i
    endif
  enddo
  !
  if (inOrder == 0 .and. .not. pipInput) then
    write(*,'(1x,a,/,a,/,a,$)') 'Your new Z''s are not in'// &
        ' monotonically increasing order.', &
        ' Enter 1 or -1 to reorder points in each object'// &
        ' by increasing or decreasing', '   new Z value,'// &
        ' or 0 not to: '
    read(*,*) ifReorder
  endif
  !
  ! loop through objects, recompute # of objects
  !
  n_object = 0
  do iobj = 1, max_mod_obj
    numInObj = npt_in_obj(iobj)
    indObj = 1
    ibase = ibase_obj(iobj)
    do while(indObj <= numInObj)
      ipnt = object(indObj + ibase)
      if (ipnt > 0 .and. ipnt <= n_point) then
        !
        ! for valid point, get new z value
        !
        izVal = nint(p_coord(3, ipnt))
        index = indexZlist(izVal + 1 - minZ)
        zNewVal = izVal
        if (index > 0) zNewVal = zNewList(index)
        !
        ! just shift values if not -999
        !
        if (zNewVal < -999 .or. zNewVal > -990) then
          p_coord(1, ipnt) = p_coord(1, ipnt) + xadd
          p_coord(2, ipnt) = p_coord(2, ipnt) + yAdd
          p_coord(3, ipnt) = p_coord(3, ipnt) + zAdd + zNewVal - izVal
          indObj = indObj + 1
          maxZ = max(maxZ, nint(p_coord(3, ipnt)))
        else
          !
          ! or delete point by moving rest of pointers in object down
          !
          numInObj = numInObj - 1
          do indMove = indObj, numInObj
            object(indMove + ibase) = object(indMove + 1 + ibase)
          enddo
        endif
      endif
    enddo
    npt_in_obj(iobj) = numInObj
    if (numInObj > 0) n_object = n_object + 1
    !
    ! if reordering, sort object array by Z value
    !
    if (ifReorder .ne. 0) then
      do i = 1, numInObj - 1
        do j = i, numInObj
          if (ifReorder * (p_coord(3, abs(object(j + ibase))) - &
              p_coord(3, abs(object(i + ibase)))) < 0.) then
            itemp = object(i + ibase)
            object(i + ibase) = object(j + ibase)
            object(j + ibase) = itemp
          endif
        enddo
      enddo
    endif
    !
  enddo
  !
  ! scale data back
  !
  call scale_model(1)
  ierr = putImodMaxes(maxX, maxY, maxZ)
  if (doThickness) ierr = putImodZscale(1.)
  call write_wmod(newModel)
  call exit(0)
end program
