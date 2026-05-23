! * * * * * COMBINEFFT * * * * * *
!
! COMBINEFFT combines the FFTs from the two tomograms of a double-axis
! tilt series, taking into account the tilt range of each tilt series
! and the transformation used to match one tomogram to the other.  For
! a location in Fourier space where there is data from one tilt series
! but not the other, it takes the Fourier value from just the one
! appropriate FFT; everywhere else it averages the Fourier values from
! the two FFT files.
!
! See man page for more details.
!
! $Id$
!
! David Mastronarde, November 1995
!
program combinefft
  implicit none
  integer NUMLOOK, LIMVIEW, LIMRING, LIMSLAB
  parameter (NUMLOOK = 16000, LIMVIEW = 1440, LIMRING = 100, LIMSLAB = 100)
  !
  integer*4 nxyz(3), mxyz(3), nxyzst(3), nxyz2(3), nx, ny, nz, mxyz2(3), nxyzIn(3)
  real*4, allocatable :: work(:)
  complex, allocatable :: array(:), brray(:)
  complex aVal
  !
  real*4 aInv(3,3)
  real*4 tiltA(LIMVIEW), tiltB(LIMVIEW), tiltDensA(LIMVIEW), tiltDensB(LIMVIEW)
  integer*4 lookupAden(-NUMLOOK:NUMLOOK), lookupBden(-NUMLOOK:NUMLOOK)
  integer*4 numInRing(LIMRING,LIMSLAB,3)
  real*4 ringSum(LIMRING,LIMSLAB,3)
  real*4 cell2(6), originX, originY, originZ

  character*320 inputFile, fileOut, lockFile
  !
  equivalence (nx, nxyz(1)), (ny, nxyz(2)), (nz, nxyz(3))
  character dat * 9, tim * 8
  character*80 titleCh
  logical ina, inb, verbose, interZone, independent, jointZone, realInput
  real*4 dmin, dmax, dmean, aCritLow, aCritHigh, bCritLow, bCritHigh, radA, radB
  integer*4 ierr, mode, iunitOut, i, j, numAviews, numBviews, iz, ind, iy, ix, ilook
  real*4 tsum, tmin, tmax, delX, delY, delZ, za, ya, yaSq, xa, xaSq, xp, yp, zp
  real*4 ratioA, ratioB, wgtA, wgtB, tmean, weightPower, facLookA, facLookB, ratioMagSq
  real*4 denRadA, denRadB, denRadSum, fRing, fSlab, ySlab, thisMean, sumTmp(3)
  integer*4 numSlabs, numRings, iSlab, iRing, iZone, minInRing, nextRing
  real*4 radiusMin, ringWidth, reduceFrac, bothMean, oneMean, target, ring
  integer*4 nextSlab, numInZero, nxChunk, nyChunk, nzChunk, ixPlace, iyPlace, izPlace
  real*4 radSq, bothRad, bothRadSq, zaSq, dmean2, yaOffset, zaOffset, dmin2, dmax2
  real*4 yaOffInit, zaOffInit, dmeanIn
  integer*4 ixLow, iyLow, izLow, ixhi, izHigh, iyHigh, nxBox, nyBox, nzBox, idim
  integer*4 nx3, ny3, nz3, mode2, ixSaveStart, ixSaveEnd, iySaveStart, iySaveEnd
  integer*4 izSaveStart, izSaveEnd
  integer*4 iiuParWrtSecPart, iiuParWrtFlushBuffers, parWrtInitialize, iiuWriteSecPart
  integer*4 b3dOutputFileType, iiTestIfHDF, b3dLockFile, iiuAltChunkSizes
  logical*4 initializeHDF, chunkedHDF, parallelHDF
  !
  logical pipInput
  integer*4 numOptArg, numNonOptArg
  integer*4 PipGetString, PipGetInteger, PipGetFloat, PipGetLogical
  integer*4 PipGetInOutFile, PipGetThreeIntegers, PipGetTwoIntegers
  !
  ! fallbacks from ../../manpages/autodoc2man -3 2  combinefft
  !
  integer numOptions
  parameter (numOptions = 29)
  character*(40 * numOptions) options(1)
  options(1) = &
      'ainput:AInputFFT:FN:@binput:BInputFFT:FN:@output:OutputFFT:FN:@'// &
      'xminmax:XMinAndMax:IP:@yminmax:YMinAndMax:IP:@zminmax:ZMinAndMax:IP:@'// &
      'taper:TaperPadsInXYZ:IT:@chunk:ChunkSizeForHDF:IT:@xsave:XSaveStartAndEnd:IP:@'// &
      'ysave:YSaveStartAndEnd:IP:@zsave:ZSaveStartAndEnd:IP:@'// &
      'place:PlaceChunkAtXYZ:IT:@atiltfile:ATiltFile:FN:@btiltfile:BTiltFile:FN:@'// &
      'ahighest:AHighestTilts:FP:@bhighest:BHighestTilts:FP:@'// &
      'inverse:InverseTransformFile:FN:@reduce:ReductionFraction:F:@'// &
      'separate:SeparateReduction:B:@joint:JointReduction:B:@ring:RingWidth:F:@'// &
      'nslabs:NumberOfSlabsInY:I:@radius:MinimumRadiusToReduce:F:@'// &
      'points:MinimumPointsInRing:I:@both:LowFromBothRadius:F:@'// &
      'verbose:VerboseOutput:B:@weight:WeightingPower:F:@param:ParameterFile:PF:@'// &
      'help:usage:B:'
  !
  weightPower = 0.
  reduceFrac = 0.
  numSlabs = 1
  ringWidth = 0.01
  radiusMin = 0.01
  minInRing = 30
  fileOut = ' '
  verbose = .false.
  jointZone = .false.
  independent = .false.
  realInput = .false.
  parallelHDF = .false.
  bothRad = 0.0
  numInZero = 0
  zaOffInit = -0.5
  yaOffInit = -0.5
  !
  ! Pip startup: set error, parse options, check help, set flag if used
  !
  call PipReadOrParseOptions(options, numOptions, 'combinefft', &
      'ERROR: COMBINEFFT - ', .true., 1, 2, 1, numOptArg, &
      numNonOptArg)
  pipInput = numOptArg + numNonOptArg > 0
  !
  ! Get input and output files
  !
  if (PipGetInOutFile('AInputFFT', 1, 'Name of first tomogram FFT file', &
      inputFile) .ne. 0) call exitError('No first input file specified')
  call imopen(1, inputFile, 'ro')
  !
  if (PipGetInOutFile('BInputFFT', 2, 'Name of second tomogram FFT file', &
      inputFile) .ne. 0) call exitError('No second input file specified')
  call imopen(2, inputFile, 'old')
  !
  ierr = PipGetInOutFile('OutputFFT', 3, &
      'Name of output file, or Return to put in 2nd file', fileOut)
  !
  call irdhdr(1, nxyz, mxyz, mode, dmin, dmax, dmean)
  call irdhdr(2, nxyz2, mxyz, mode2, dmin2, dmax2, dmean2)
  if (nx .ne. nxyz2(1) .or. ny .ne. nxyz2(2) .or. nz .ne. nxyz2(3)) &
      call exitError('The two files are not the same size')
  nxyzIn(:) = nxyz(:)
  dmeanIn = dmean
  !
  if (mode .ne. 4 .or. mode2 .ne. 4) then
    realInput = .true.
    if (mode == 4 .or. mode2 == 4) call exitError( &
        'Both files must be either real data or FFTs')
    if (fileOut == ' ') call exitError( &
        'You must enter an output file for real input data')
    if (.not. pipInput) call exitError('You can use real data only with PIP input')
    call taperPrep(pipInput, .false., nxyz, ixLow, ixhi, iyLow, iyHigh, izLow, &
        izHigh, nxBox, nyBox, nzBox, nx3, ny3, nz3, nxyz2, mxyz2, cell2, &
        originX, originY, originZ)
    !
    ! these sizes below must be the same as if using FFT input
    nx = nx3 / 2 + 1
    ny = ny3
    nz = nz3
    zaOffInit = 0.
    yaOffInit = 0.
    !
    ! Promote the mode
    if (mode == 2 .or. mode2 == 2) then
      mode = 2
    elseif (mode == 0 .or. mode2 == 0) then
      mode = max(mode, mode2)
    endif
  endif

  initializeHDF = PipGetThreeIntegers('ChunkSizeForHDF', nxChunk, nyChunk, nzChunk) == 0
  ierr = PipGetTwoIntegers('XSaveStartAndEnd', ixSaveStart, ixSaveEnd) + &
      PipGetTwoIntegers('YSaveStartAndEnd', iySaveStart, iySaveEnd) + &
      PipGetTwoIntegers('ZSaveStartAndEnd', izSaveStart, izSaveEnd) + &
      PipGetThreeIntegers('PlaceChunkAtXYZ', ixPlace, iyPlace, izPlace)
  chunkedHDF = ierr == 0 .and. .not. initializeHDF
  if (ierr > 0 .and. ierr .ne. 4 .and. .not. initializeHDF) call exitError( &
      'For writing to a chunked HDF file,'// &
      ' -xsave, -ysave, -zsave, and -place must all be entered')
  if (chunkedHDF) &
      parallelHDF = PipGetString('LockFileForHDF', lockFile) == 0
  if ((initializeHDF .or. chunkedHDF) .and. .not. realInput) call exitError( &
      'You must have real input data for writing to an HDF file')

  if (initializeHDF) then
    call overrideOutputType(5)
    if (b3dOutputFileType() .ne. 5) call exitError( &
        'Chunks cannot be used; this package was not built with HDF support')
  else
    if (float(nx * ny) * nz > 1.e9) call exitError('Volume to be combined is too large')
    idim = nx * ny * nz
    allocate(array(idim), brray(idim), work(2 * nx * nz), stat = ierr)
    call memoryError(ierr, 'arrays for volumes of data')
    print *,'allocation in reals:', 2 * idim
  endif
  !
  ! Set up the output file
  iunitOut = 2
  call b3ddate(dat)
  call time(tim)
  !
  write(titleCh, 3000) dat, tim
3000 format ( 'COMBINEFFT: Combined FFT from two tomograms',t57,a9, 2x,a8)
  !
  if (fileOut .ne. ' ') then
    iunitOut = 3
    if (chunkedHDF) then
      if (iiTestIfHDF(fileOut) <= 0) call exitError( &
          'Chunk writing options were entered but the output file is not an HDF file')
      if (parallelHDF) then
        ierr = parWrtInitialize(lockFile, 5, -nx, ny, nz)
        if (ierr .ne. 0) then
          write(*,'(/,a,i3)') 'ERROR: COMBINEFFT - Initializing parallel write '// &
              'lock file, error', ierr
          call exit(1)
        endif
        call parWrtProperties(ix, iy, iz)
        if (b3dLockFile(ix) .ne. 0) &
            call exitError('Could not get lock for opening HDF file')
      endif
      call imopen(3, fileOut, 'old')
      call irdhdr(3, nxyz2, mxyz, mode2, dmin, dmax, dmean2)
      if (nxyzIn(1) .ne. nxyz2(1) .or. nxyzIn(2) .ne. nxyz2(2) .or.  &
          nxyzIn(3) .ne. nxyz2(3) .or. mode2 .ne. mode) call exitError( &
          'The existing output file does not have right size or mode')
      if (parallelHDF) call iiuParWrtRecloseHDF(3, 0)

    else
      call imopen(3, fileOut, 'new')
      call iiuTransHeader(3, 1)
      if (realInput) then
        !
        call iiuAltMode(3, mode)
        if (initializeHDF) then
          !
          ! Initializing an HDF: set the chunk sizes, write the dummy section, and
          ! write header and exit
          if (iiuAltChunkSizes(3, nxChunk, nyChunk, nzChunk) .ne. 0)  &
              call exitError('Setting up chunks in the HDF output file')
          call iiuWriteDummySecToHDF(3)
          call iiuWriteHeaderStr(3, titleCh, 1, dmean + 1., dmean - 1., dmean)
          call iiuClose(3)
          call exit(0)
        else
          nxyzst = 0
          call iiuAltSize(3, nxyz2, nxyzst)
          call iiuAltSample(3, mxyz2)
          call iiuAltCell(3, cell2)
          call iiuAltOrigin(3, originX, originY, originZ)
        endif
      endif
    endif
  endif

  inputFile = ' '
  if (pipInput) then
    ierr = PipGetString('InverseTransformFile', inputFile)
  else
    write(*,'(1x,a,$)') &
        'File with inverse of matching transformation: '
    read(5, '(a)') inputFile
  endif
  if (inputFile == ' ') call exitError('No file specified with inverse transformation')
  call dopen(1, inputFile, 'ro', 'f')
  do i = 1, 3
    read(1,*) (aInv(i, j), j = 1, 3)
  enddo
  close(1)
  !
  call getTilts(pipInput, 'AHighestTilts', 'ATiltFile', 'first', &
      numAviews, tiltA, LIMVIEW, lookupAden, NUMLOOK, facLookA, tiltDensA, &
      aCritLow, aCritHigh)
  call getTilts(pipInput, 'BHighestTilts', 'BTiltFile', 'second', &
      numBviews, tiltB, LIMVIEW, lookupBden, NUMLOOK, facLookB, tiltDensB, &
      bCritLow, bCritHigh)
  if (pipInput) then
    ierr = PipGetFloat('WeightingPower', weightPower)
    ierr = PipGetFloat('ReductionFraction', reduceFrac)
    if (reduceFrac > 10.) call exitError('Reduction fraction must not be bigger than 10')
    ierr = PipGetLogical('SeparateReduction', independent)
    ierr = PipGetLogical('JointReduction', jointZone)
    interZone = .not.(jointZone .or. independent)
    ierr = PipGetLogical('VerboseOutput', verbose)
    ierr = PipGetFloat('LowFromBothRadius', bothRad)
  endif
  !
  ! set up reduction
  !
  if (reduceFrac > 0.) then
    ierr = PipGetFloat('RingWidth', ringWidth)
    ierr = PipGetFloat('MinimumRadiusToReduce', radiusMin)
    ierr = PipGetInteger('NumberOfSlabsInY', numSlabs)
    ierr = PipGetInteger('MinimumPointsInRing', minInRing)
    if (numSlabs > LIMSLAB) call exitError('Too many slabs in Y for arrays')
    numRings = (0.9 - radiusMin) / ringWidth + 2
    if (numRings > LIMRING) call exitError( &
        'Too many rings for arrays with this ring width')
    if (ringWidth <= 0) call exitError('Illegal entry for ring width')
    if (numSlabs <= 0) call exitError('Illegal entry for number of slabs')
    if (minInRing <= 5) call exitError( &
        'Minimum number of points in ring is too small to use')
    if (radiusMin < 0) call exitError('Minimum radius must be positive')

    do iy = 1, numSlabs
      do ix = 1, numRings
        do iz = 1, 3
          numInRing(ix, iy, iz) = 0
          ringSum(ix, iy, iz) = 0.
        enddo
      enddo
    enddo
  endif
  !
  call PipDone()
  !
  tsum = 0.
  tmin = 1.e30
  tmax = -1.e30
  delX = 0.5 / (nx - 1.)
  delY = 1. / ny
  delZ = 1. / nz
  bothRadSq = bothRad**2
  !
  ! read real data (supply volume mean for testing to see if it comes out
  ! the same as from taperoutvol)
  if (realInput) then
    call readTaperTransform(1, array, work, ixLow, ixhi, iyLow, iyHigh, &
        izLow, izHigh, nxBox, nyBox, nzBox, nx3, ny3, nz3, 0, dmean)
    call readTaperTransform(2, brray, work, ixLow, ixhi, iyLow, iyHigh, &
        izLow, izHigh, nxBox, nyBox, nzBox, nx3, ny3, nz3, 0, dmean2)
  endif
  !
  ! Loop on Z, get index of start of plane in arrays
  zaOffset = zaOffInit
  do iz = 1, nz
    ind = 1 + nx * ny * (iz - 1)
    !
    ! Still read FFT data plane by plane
    if (.not. realInput) then
      call irdsec(1, array(ind),*99)
      call iiuSetPosition(2, iz - 1, 0)
      call irdsec(2, brray(ind),*99)
    endif
    if (realInput .and. iz > nz / 2) zaOffset = -1.
    ! DO THIS IN MTFFLITER
    ! if (iz > nz / 2) za = za - 1.
    za = delZ * (iz - 1.) + zaOffset
    zaSq = za**2
    !
    ! Loop on Y
    yaOffset = yaOffInit
    do iy = 1, ny
      if (realInput .and. iy > ny / 2) yaOffset = -1.
      ya = delY * (iy - 1.) + yaOffset
      yaSq = ya**2
      !
      ! Loop on X
      do ix = 1, nx
        xa = delX * (ix - 1.)
        xaSq = xa**2
        !
        ! back transform this position to get vector in fft b
        !
        xp = aInv(1, 1) * xa + aInv(1, 2) * ya + aInv(1, 3) * za
        yp = aInv(2, 1) * xa + aInv(2, 2) * ya + aInv(2, 3) * za
        if (xp < 0.) then
          xp = -xp
          yp = -yp
        endif
        radSq = zaSq + xaSq + yaSq
        !
        ratioA = ya / max(xa, 1.e-6)
        ratioB = yp / max(xp, 1.e-6)
        ina = (ratioA >= aCritLow .and. ratioA <= aCritHigh) .or. radSq < bothRadSq
        inb = (ratioB >= bCritLow .and. ratioB <= bCritHigh) .or. radSq < bothRadSq
        !
        ! For counting pixels
        ! ina=(rata>=acritlo.and.rata<=acrithi)
        ! inb=(ratb>=bcritlo.and.ratb<=bcrithi)
        ! if (.not.(ina.and.inb) .and. radSq<bothRadSq) then
        ! ina = .true.
        ! inb = .true.
        ! numInZero = numInZero + 1
        ! endif
        aVal = array(ind)
        ! array(ind) =(2., 0.)
        if (.not.ina .and. .not.inb) then
          !
          ! if in neither, take simple mean
          !
          array(ind) = 0.5 * (array(ind) + brray(ind))
          ! array(ind) =(2.5, 0.)
        elseif (.not.ina .and. inb) then
          !
          ! if in B alone, take b's value
          !
          array(ind) = brray(ind)
          ! array(ind) =(3., 0.)
        elseif (ina .and. inb) then
          !
          ! in both: need to mix in selected way
          !
          wgtA = 0.5
          wgtB = 0.5
          if (weightPower > 0.001 .and. radSq >= bothRadSq) then
            !
            ! weighting by local density:  Find radius in A and B.
            ! get change in magnitude and make the magnitude change by
            ! the same amount in opposite direction, since stretching in
            ! real space is squeezing in Fourier space (the square is
            ! needed to get from stretch through null to squeeze) .
            !
            zp = aInv(3, 1) * xa + aInv(3, 2) * ya + aInv(3, 3) * za
            ratioMagSq = (xaSq + yaSq + zaSq) / (xp**2 + yp**2 + zp**2)
            radA = sqrt(xaSq + yaSq)
            radB = sqrt(xp**2 + yp**2) * ratioMagSq
            !
            ! find tilt density and
            ! effectively divide by radius (multiply by other radius)
            !
            ilook = nint(facLookA * ratioA)
            if (abs(ilook) > NUMLOOK) print *,ratioA, xa, ya, za, xp, yp
            denRadA = (radB * tiltDensA(lookupAden(ilook)))**weightPower
            ilook = nint(facLookB * ratioB)
            denRadB = (radA * tiltDensB(lookupBden(ilook)))**weightPower
            denRadSum = denRadA + denRadB
            if (denRadSum > 1.e-4) then
              wgtA = denRadA / denRadSum
              wgtB = denRadB / denRadSum
            endif
          endif
          ! array(ind) =(wa, 0.)
          array(ind) = wgtA * array(ind) + wgtB * brray(ind)
          !
          ! else if in A and not in B, leave A value as is
          !
        endif
        !
        ! if reducing, compute ring, slab, and zone and add abs value
        !
        if (reduceFrac > 0.) then
          iSlab = max(1, min(numSlabs, int((ya + 0.5) * numSlabs) + 1))
          radA = sqrt(xaSq + yaSq + zaSq)
          iRing = int((radA - radiusMin) / ringWidth) + 1
          if (interZone) then
            !
            ! If doing comparisons between zones, determine zone and
            ! sum specifically for the zones
            !
            iZone = 0
            if (ina .and. .not.inb) iZone = 1
            if (ina .and. inb) iZone = 2
            if (.not.ina .and. inb) iZone = 3
            if (iRing > 0 .and. iZone > 0) then
              numInRing(iRing, iSlab, iZone) = &
                  numInRing(iRing, iSlab, iZone) + 1
              ringSum(iRing, iSlab, iZone) = &
                  ringSum(iRing, iSlab, iZone) + cabs(array(ind))
            endif
          else
            !
            ! Otherwise, just do joint area and sum A, B, and average
            !
            if (iRing > 0 .and. ina .and. inb) then
              numInRing(iRing, iSlab, 2) = numInRing(iRing, iSlab, 2) + 1
              ringSum(iRing, iSlab, 1) = ringSum(iRing, iSlab, 1) + &
                  cabs(aVal)
              ringSum(iRing, iSlab, 2) = ringSum(iRing, iSlab, 2) + &
                  cabs(array(ind))
              ringSum(iRing, iSlab, 3) = ringSum(iRing, iSlab, 3) + &
                  cabs(brray(ind))
            endif
          endif
        endif
        ind = ind + 1
      enddo
    enddo
  enddo
  !
  ! If reducing, first get the scaling factors for the rings
  !
  if (reduceFrac > 0.) then
    if (verbose) print *,'Ring  Slab  Zone  Mixed mean  Single mean', &
        '    Target   Reduction factor'
    do ix = 1, numRings
      do iy = 1, numSlabs
        if (.not.interZone) then
          numInRing(ix, iy, 1) = numInRing(ix, iy, 2)
          numInRing(ix, iy, 3) = numInRing(ix, iy, 2)
        endif
        sumTmp(1) = ringSum(ix, iy, 1)
        sumTmp(3) = ringSum(ix, iy, 3)
        do iz = 1, 3, 2
          if (numInRing(ix, iy, iz) >= minInRing .and. &
              numInRing(ix, iy, 2) >= minInRing) then
            bothMean = ringSum(ix, iy, 2) / numInRing(ix, iy, 2)
            thisMean = sumTmp(iz) / numInRing(ix, iy, iz)
            oneMean = thisMean
            if (.not.independent .and. .not.interZone) oneMean = 0.5 * &
                (sumTmp(1) + sumTmp(3)) / numInRing(ix, iy, 2)
            target = max(0., (1. - reduceFrac) * oneMean + &
                reduceFrac * bothMean)
            ringSum(ix, iy, iz) = min(1., target / oneMean)
            if (verbose) write(*,107) &
                ix, iy, iz, bothMean, thisMean, target, ringSum(ix, iy, iz)
107         format(i4,2i6,1x,3f12.4,f8.4)
          else
            ringSum(ix, iy, iz) = 1.
          endif
        enddo
      enddo
    enddo
    !
    ! Now run through the volume again
    !
    zaOffset = zaOffInit
    do iz = 1, nz
      ind = 1 + nx * ny * (iz - 1)
      if (realInput .and. iz > nz / 2) zaOffset = -1.
      za = delZ * (iz - 1.) + zaOffset
      yaOffset = yaOffInit
      do iy = 1, ny
        if (realInput .and. iy > ny / 2) yaOffset = -1.
        ya = delY * (iy - 1.) + yaOffset
        yaSq = ya**2
        ySlab = (ya + 0.5) * numSlabs
        iSlab = int(ySlab + 0.5)
        fSlab = ySlab + 0.5 - iSlab
        nextSlab = min(numSlabs, iSlab + 1)
        iSlab = max(1, iSlab)
        do ix = 1, nx
          !
          ! Find out where pixel is again
          !
          xa = delX * (ix - 1.)
          xaSq = xa**2
          xp = aInv(1, 1) * xa + aInv(1, 2) * ya + aInv(1, 3) * za
          yp = aInv(2, 1) * xa + aInv(2, 2) * ya + aInv(2, 3) * za
          if (xp < 0.) then
            xp = -xp
            yp = -yp
          endif
          !
          ratioA = ya / max(xa, 1.e-6)
          ratioB = yp / max(xp, 1.e-6)
          ina = ratioA >= aCritLow .and. ratioA <= aCritHigh
          inb = ratioB >= bCritLow .and. ratioB <= bCritHigh
          radA = sqrt(xaSq + yaSq + za**2)
          iZone = 0
          if (ina .and. .not.inb) iZone = 1
          if (.not.ina .and. inb) iZone = 3
          if (iZone > 0 .and. radA >= radiusMin) then
            !
            ! find ring and slab and adjust when in A or B only
            !
            ring = (radA - radiusMin) / ringWidth
            iRing = int(ring + 0.5)
            fRing = ring + 0.5 - iRing
            nextRing = min(numRings, iRing + 1)
            iRing = max(1, iRing)

            array(ind) = array(ind) * &
                ((1 - fRing) * (1 - fSlab) * ringSum(iRing, iSlab, iZone) &
                + (1 - fRing) * fSlab * ringSum(iRing, nextSlab, iZone) + &
                fRing * (1 - fSlab) * ringSum(nextRing, iSlab, iZone) + &
                fRing * fSlab * ringSum(nextRing, nextSlab, iZone))
          endif
          ind = ind + 1
        enddo
      enddo
    enddo
  endif
  !
  ! Write the data out, taking inverse FFT and repacking real data first
  if (realInput) call thrdfft(array, work, nx3, ny3, nz3, -1)
  
  do iz = 0, nz - 1
    if (.not. chunkedHDF .or. (iz >= izSaveStart .and. iz <= izSaveEnd)) then
      ind = 1 + nx * ny * iz
      if (realInput) then
        call irepak(array(ind), array(ind), nx3 + 2, ny, 0, nx3 - 1, 0, ny - 1)
        call iclden(array(ind), nx3, ny, 1, nx3, 1, ny, dmin2, dmax2, dmean2)
      else
        call iclcdn(array(ind), nx, ny, 1, nx, 1, ny, dmin2, dmax2, dmean2)
      endif
      tmin = min(tmin, dmin2)
      tmax = max(tmax, dmax2)
      tsum = tsum + dmean2
      if (chunkedHDF) then
        if (parallelHDF) then
          call parWrtPosn(3, izPlace + iz - izSaveStart, iyPlace)
          ierr = iiuParWrtSecPart(3, array(ind), nx3, ixSaveStart, ixPlace,  &
              ixPlace + ixSaveEnd - ixSaveStart, iySaveStart, iySaveEnd)
        else
          call iiuSetPosition(3, izPlace + iz - izSaveStart, iyPlace)
          ierr = iiuWriteSecPart(3, array(ind), nx3, ixSaveStart, ixPlace,  &
              ixPlace + ixSaveEnd - ixSaveStart, iySaveStart, iySaveEnd)
        endif
        if (ierr .ne. 0) &
            call exitError('Writing to chunk in HDF file')
      else
        call iiuSetPosition(iunitOut, iz, 0)
        call iiuWriteSection(iunitOut, array(ind))
      endif
    endif
  enddo

  tmean = tsum / nz
  if (parallelHDF) then
    if (iiuParWrtFlushBuffers(3) .ne. 0) &
        call exitError("Finishing writing to output HDF file")
    call parWrtClose()
    write(*,'(a,3g15.7,f15.0)') 'Min, max, mean, # pixels=', tmin, tmax, tmean, &
        float(nx3) * ny3 * nz3
  else
    if (chunkedHDF) then
      if (dmin < dmax) then
        tmin = min(dmin, tmin)
        tmax = max(dmax, tmax)
      endif
      tmean = dmeanIn
    endif
    call iiuWriteHeaderStr(iunitOut, titleCh, 1, tmin, tmax, tmean)
  endif
  call iiuClose(iunitOut)
  ! print *,numInZero, ' points added to joint area near zero radius'
  call exit(0)
99 call exitError( 'Reading FFT file')
end program combinefft



! GETTILTS gets tilt angles and/or cutoffs for tangent of high angles
! PIPINPUT is a logical, true for PIP input
! ANGLEOPT is the name of the option for entering highest angles
! FILEOPT is the name of the option for entering a tilt angle file
! WHICH has 'first' or 'second'
! NVIEW is returned with the number of angles read, or zero
! TILT is returned with tilt angles
! LIMVIEW is the size of the angle arrays
! LOOK is a filled with a lookup table from tangents to densities,
! it is dimensioned to -NUMLOOK:NUMLOOK,
! FACLOOK is returned with a factor to use for the lookup
! DENS is returned with relative tilt densities
! CRITLO and CRITHI are returned with criterion tangents of highest
! angles
!
subroutine getTilts(pipInput, angleOpt, fileOpt, which, nview, tilt, &
    LIMVIEW, look, NUMLOOK, facLook, dens, critLow, critHigh)
  implicit none
  logical pipInput
  character*(*) angleOpt, fileOpt, which
  integer*4 NUMLOOK, look(-NUMLOOK:NUMLOOK), nview, LIMVIEW
  real*4 dens(*), tilt(*), wgtIncrem(20), critLow, critHigh, tiltLow, tiltHigh, facLook
  character*120 line
  integer*4 ierr, ierr2, numWeights, i, j, iv, iw, minLook, maxLook, indLook
  integer*4 nextLook, mid, ilook
  real*4 tmp, sumIntrv, wsum, avgIntrv
  integer*4 PipGetString, PipGetTwoFloats
  real*4 tand
  logical line_is_filename
  !
  numWeights = 2
  !
  nview = 0
  tiltHigh = -9999.
  line = ' '

  if (pipInput) then
    ierr = PipGetTwoFloats(angleOpt, tiltLow, tiltHigh)
    ierr2 = PipGetString(fileOpt, line)
    if (ierr == 0 .and. ierr2 == 0) call exitError('You '// &
        'cannot enter both highest angles and a tilt angle file')
    if (ierr .ne. 0 .and. ierr2 .ne. 0) call exitError('You must'// &
        'enter either highest angles or a tilt angle file')
    if (ierr == 0) go to 20
  else
    print *,'For ', which, ' tomogram file, enter either the ', &
        'starting and ending tilt ', &
        ' angles, or the name of a file with tilt angles in it'
    read(5, '(a)') line
    if (line_is_filename(line)) go to 10
    read(line,*,err = 10, end = 10) tiltLow, tiltHigh
    go to 20
  endif
  !
  ! get here either way if line has a filename for tilt angle file
  !
10 if (line == ' ') call exitError('No tilt angle filename entered')
  call dopen(1, line, 'ro', 'f')
15 read(1,*,err = 25, end = 18) tilt(nview + 1)
  nview = nview + 1
  if (nview >= LIMVIEW) call exitError('Too many tilt angles for arrays')
  go to 15
18 if (nview < 2) call exitError('Too few tilt angles in file')
  close(1)
  tiltLow = tilt(1)
  tiltHigh = tilt(nview)
  !
  ! INVERT TILT ANGLES BECAUSE "TILT" PROGRAM IS WEIRD
  !
  do i = 1, nview
    tilt(i) = -tilt(i)
  enddo
  !
  ! order tilt angles to make it easier to get densities
  !
  do i = 1, nview - 1
    do j = i + 1, nview
      if (tilt(i) > tilt(j)) then
        tmp = tilt(i)
        tilt(i) = tilt(j)
        tilt(j) = tmp
      endif
    enddo
  enddo
  !
  ! compute densities just as in Tilt program
  !
  do i = 1, numWeights
    wgtIncrem(i) = 1. / (i - 0.5)
  enddo
  avgIntrv = (tilt(nview) - tilt(1)) / (nview - 1)
  do iv = 1, nview
    sumIntrv = 0
    wsum = 0.
    do iw = 1, numWeights
      if (iv - iw > 0) then
        wsum = wsum + wgtIncrem(iw)
        sumIntrv = sumIntrv + wgtIncrem(iw) * (tilt(iv + 1 - iw) - &
            tilt(iv - iw))
      endif
      if (iv + iw <= nview) then
        wsum = wsum + wgtIncrem(iw)
        sumIntrv = sumIntrv + wgtIncrem(iw) * (tilt(iv + iw) - &
            tilt(iv + iw - 1))
      endif
    enddo
    dens(iv) = avgIntrv / (sumIntrv / wsum)
  enddo
  !
  ! build lookup table to tilt angles
  !
  facLook = (NUMLOOK - 10.) / tand(max(abs(tilt(1)), abs(tilt(nview))))
  do i = -NUMLOOK, NUMLOOK
    look(i) = 0
  enddo
  minLook = NUMLOOK
  maxLook = -NUMLOOK
  !
  ! fill in values at the angles
  !
  do i = 1, nview
    indLook = nint(facLook * tand(tilt(i)))
    look(indLook) = i
    minLook = min(minLook, indLook)
    maxLook = max(maxLook, indLook)
  enddo
  !
  ! extend endpoints
  !
  do i = -NUMLOOK, minLook - 1
    look(i) = look(minLook)
  enddo
  do i = maxLook + 1, NUMLOOK
    look(i) = look(maxLook)
  enddo
  ilook = minLook
  !
  ! fill in each interval split between neighbors
  !
  do while(ilook < maxLook)
    nextLook = ilook + 1
    do while (look(nextLook) == 0)
      nextLook = nextLook + 1
    enddo
    mid = (nextLook + ilook) / 2
    do i = ilook + 1, mid
      look(i) = look(ilook)
    enddo
    do i = mid + 1, nextLook - 1
      look(i) = look(nextLook)
    enddo
    ilook = nextLook
  enddo
  !
20 if (tiltHigh == -9999.) call exitError('Highest tilt angle not entered')
  !
  ! INVERT ANGLES HERE TOO
  !
  critLow = tand(min(-tiltLow, -tiltHigh))
  critHigh = tand(max(-tiltLow, -tiltHigh))
  !
  ! If there were no tilt angles, just set up for equal densities
  !
  if (nview == 0) then
    facLook = (NUMLOOK - 10.) / max(abs(critLow), abs(critHigh))
    do i = -NUMLOOK, NUMLOOK
      look(i) = 1
    enddo
    dens(1) = 1.
  endif
  return
25 call exitError('Reading tilt angle file')
end subroutine getTilts


! readTaperTransform reads a subvolume from a file, pads and tapers it
! outside into a larger volume, and takes the FFT
! IUNIT is the unit number
! ARRAY is the array for the volume
! WORK is a work array for 3D FFTs
! IXLO, IXHI, IYLO, IYHI, IZLO, IZHI are the index coordinates of the
! subvolume in the file
! NXBOX, NYBOX, NZBOX are the size of the box being read in
! NX3, NY3, NZ3 are the size of the padded volume
! if IFMEAN is 1 it will taper to DMEANIN, otherwise it will use the
! mean of the faces of the subvolume
! Planes of the FFT consist of (NX3 + 2) * NY floats consecutively
subroutine readTaperTransform(iunit, array, work, ixLow, ixhi, iyLow, iyHigh, &
    izLow, izHigh, nxBox, nyBox, nzBox, nx3, ny3, nz3, ifMean, dmeanIn)
  implicit none
  real*4 array(*), work(*), atten, dmin, dmax, dmean, base, dmeanIn
  integer*4 iunit, ixLow, ixhi, iyLow, iyHigh, izLow, izHigh, nxBox, nyBox
  integer*4 nzBox, nx3, ny3, nz3, nxDim, izStart, izEnd, iz, izRead, ibase
  integer*4 iy, ix, ixBase, i, numPix, ifMean, ioffset
  real*8 edgeMean, edgeSum
  real*8 sliceEdgeMean
  nxDim = nx3 + 2
  izStart = izLow - ((nz3 - nzBox) / 2)
  izEnd = izStart + nz3 - 1
  !
  ! Read all the data first to get the mean
  edgeSum = 0.
  numPix = 0
  do iz = izLow, izHigh
    ibase = nxDim * ny3 * (iz - izStart)
    call iiuSetPosition(iunit, iz, 0)
    call irdpas(iunit, array(ibase + 1), nxBox, nyBox, ixLow, ixhi, iyLow, iyHigh, *99)
    if (iz == izLow .or. iz == izHigh) then
      call iclden(array(ibase + 1), nxBox, nyBox, 1, nxBox, 1, nyBox, dmin, &
          dmax, dmean)
      numPix = numPix + nxBox * nyBox
      edgeSum = edgeSum + nxBox * nyBox * dmean
    else
      edgeMean = sliceEdgeMean(array(ibase + 1), nxBox, 1, nxBox, 1, nyBox)
      numPix = numPix + 2 * (nxBox + nyBox) - 4
      edgeSum = edgeSum + edgeMean * (2 * (nxBox + nyBox) - 4)
    endif
  enddo
  dmean = edgeSum / numPix
  print *,'file mean', dmeanIn, '    edge mean', dmean
  if (ifMean == 0) dmeanIn = dmean
  !
  ! taper now that mean is known
  do iz = izLow, izHigh
    ibase = nxDim * ny3 * (iz - izStart)
    call taperOutPad(array(ibase + 1), nxBox, nyBox, array(ibase + 1), nxDim, nx3, &
        ny3, 1, dmeanIn)
  enddo
  do iz = izStart, izEnd
    izRead = max(izLow, min(izHigh, iz))
    ibase = nxDim * ny3 * (iz - izStart)
    ioffset = nxDim * ny3 * (izRead - iz)
    if (iz < izLow .or. iz > izHigh) then
      if (iz < izLow) then
        atten = float(iz - izStart) / (izLow - izStart)
      else
        atten = float(izEnd - iz) / (izEnd - izHigh)
      endif
      base = (1. -atten) * dmeanIn
      do iy = 1, ny3
        ixBase = ibase + (iy - 1) * nxDim
        do i = ixBase + 1, ixBase + nx3
          array(i) = base + atten * array(i + ioffset)
        enddo
      enddo
    endif
  enddo
  !
  ! Take fft
  call thrdfft(array, work, nx3, ny3, nz3, 0)
  return
99 call exitError('Reading image file')
end subroutine readTaperTransform
