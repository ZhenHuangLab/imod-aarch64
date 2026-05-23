! Assesses making a regular step (a jump) in the local reprojection, and then
! does the steps of the jump.  This operation took twice as in C++ with the Intel
! compiler as with the Intel Fortran compiler
!
subroutine projSumLocal(xx, yy, zz, sum, xproj, yproj, array, xProjFs, xProjZs, yProjFs, &
    yProjZs, numWarpDelz, dxWarpDelz, warpDelz, ithickReproj, sinBeta, nxLoad, &
    inLoadStart, inLoadEnd, inPlaneSize, zJump, ycenAdj)
  implicit none
  real*4 xproj, yproj, zz, xx, yy, xProjFs(*), xProjZs(*), yProjFs(*), yProjZs(*)
  real*4 array(*), warpDelz(*), dxWarpDelz, sinBeta, zJump, ycenAdj  
  integer*4 nxLoad, inLoadStart, inLoadEnd, inPlaneSize, numWarpDelz, ithickReproj
  real*8 sum
  real*4 xxGood, yyGood, zzGood, delX, delY, delZ, fx, oneMfx, fy, oneMfy, fz, oneMfz
  real*4 d11, d12, d21, d22
  integer*4 ind, numJump, indJump, ix, iy, iz
  logical*4 tryJump

  tryJump = .true.
  do while(tryJump)
    !
    ! If jumping is OK, save the current position and compute
    ! how many steps can be jumped, stopping below the top
    xxGood = xx
    yyGood = yy
    zzGood = zz
    ind = max(1., min(float(numWarpDelz), xx / dxWarpDelz))
    delZ = warpDelz(ind)
    numJump = zJump / delZ
    if (zz + zJump > ithickReproj - ycenAdj - 1) then
      numJump = (ithickReproj - ycenAdj - 1 - zz) / delZ
      tryJump = .false.
    endif
    if (numJump > 0) then
      !
      ! Make the jump, find the projecting point;
      ! if it's out of bounds restore last point
      zz = zz + numJump * delZ
      xx = xx + numJump * sinBeta
      call loadedProjectingPoint(xproj, yproj, zz, nxLoad, inLoadStart, inLoadEnd, &
          xProjFs, xProjZs, yProjFs, yProjZs, xx, yy)
      if (yy < inLoadStart .or. yy > inLoadEnd .or. xx < 1. .or. xx >= nxLoad) then
        numJump = 0
        xx = xxGood
        yy = yyGood
        zz = zzGood
        tryJump = .false.
      else
        delX = (xx - xxGood) / numJump
        delY = (yy - yyGood) / numJump
      endif
    endif
    !
    ! Loop on points from last one to final one
    !
    do indJump = 1, numJump
      xx = xxGood + indJump * delX
      yy = yyGood + indJump * delY
      zz = zzGood + indJump * delZ
      ix = xx
      fx = xx - ix
      oneMfx = 1. - fx
      iy = min(int(yy), inLoadEnd - 1)
      fy = yy - iy
      oneMfy = 1. - fy
      ! BUG again, need ycenAdj
      iz = zz + ycenAdj
      fz = zz + ycenAdj - iz
      oneMfz = 1. - fz
      d11 = oneMfx * oneMfy
      d12 = oneMfx * fy
      d21 = fx * oneMfy
      d22 = fx * fy
      ind = inPlaneSize * (iy - inLoadStart) + (iz - 1) * nxLoad + ix
      sum = sum + oneMfz * (d11 * array(ind) &
          + d12 * array(ind + inPlaneSize) + d21 * array(ind + 1) &
          + d22 * array(ind + inPlaneSize + 1)) &
          + fz * (d11 * array(ind + nxLoad) &
          + d12 * array(ind + inPlaneSize + nxLoad) &
          + d21 * array(ind + 1 + nxLoad) &
          + d22 * array(ind + inPlaneSize + 1 + nxLoad))
      ! fx = xx
      ! fy = yy
      ! call loadedProjectingPoint(xproj, yproj, zz, &
      ! nxload, inloadstr, inloadend, fx, fy)
      ! diffxmax = max(diffxmax , abs(fx - xx))
      ! diffymax = max(diffymax , abs(fy - yy))
    enddo
  enddo
  return
end subroutine projSumLocal

! Finds loaded point that projects to xproj, yproj at centered Z value
! zz, using stored values for [xy]zfac[fv].
! Takes starting value in xx, yy and returns found value.
! X coordinate needs to be a loaded X index
! Y coordinate yy is in slices of reconstruction, yproj in original proj
! And this also runs slower in C, aside from being needed to call from projSumLocal
!
subroutine loadedProjectingPoint(xproj, yproj, zz, nxLoad, inLoadStart, inLoadEnd,  &
    xProjFs, xProjZs, yProjFs, yProjZs, xx, yy)
  implicit none
  real*4 xproj, yproj, zz, xx, yy, xProjFs(*), xProjZs(*), yProjFs(*), yProjZs(*)
  integer*4 nxLoad, inLoadStart, inLoadEnd
  integer*4 iter, ifDone, ind, ix, iy, ifOut, i
  real*4 xp11, yp11, xp12, yp12, xp21, yp21, xErr, yErr, dypx, dxpy, dxpx
  real*4 dypy, den, fx, fy
  ! logical*4 dbout
  ! dbout = abs(xproj - 700.) < 3 .and. abs(zz) < 3
  ! dbout = .false.
  ! print *,'Finding proj pt to', xproj, yproj, zz
  iter = 0
  ifDone = 0
  do while (ifDone == 0 .and. iter < 5)
    ix = floor(xx)
    iy = floor(yy)
    ifOut = 0
    if (ix < 1 .or. ix >= nxLoad .or. iy < inLoadStart .or. &
        iy >= inLoadEnd) then
      ifOut = 1
      ix = min(nxLoad - 1, max(1, ix))
      iy = min(inLoadEnd - 1, max(inLoadStart, iy))
    endif
    ind = nxLoad * (iy - inLoadStart) + ix
    xp11 = xProjFs(ind) + xProjZs(ind) * zz
    yp11 = yProjFs(ind) + yProjZs(ind) * zz
    xp21 = xProjFs(ind + 1) + xProjZs(ind + 1) * zz
    yp21 = yProjFs(ind + 1) + yProjZs(ind + 1) * zz
    xp12 = xProjFs(ind + nxLoad) + xProjZs(ind + nxLoad) * zz
    yp12 = yProjFs(ind + nxLoad) + yProjZs(ind + nxLoad) * zz
    ! write(*,101) 'facs', (xprojfs(i), xprojzs(i), yprojfs(i), &
    ! yprojzs(i), i=ind, ind+1)
    ! write(*,101) 'xps', xx, yy, zz, xp11, yp11, xp21, yp21, xp12, yp12
    !101     format(a,9f8.2)
    xErr = xproj - xp11
    yErr = yproj - yp11
    dxpx = xp21 - xp11
    dxpy = xp12 - xp11
    dypx = yp21 - yp11
    dypy = yp12 - yp11
    den = dxpx * dypy - dxpy * dypx
    fx = (xErr * dypy - yErr * dxpy) / den
    fy = (dxpx * yErr - dypx * xErr) / den
    ! write(*,101) 'dx,err,f', dxpx, dxpy, dypx, dypy, den, xerr, yerr, fx, fy
    xx = ix + fx
    yy = iy + fy
    if (fx > -0.1 .and. fx < 1.1 .and. fy > -0.1 .and. &
        fy < 1.1) ifDone = 1
    if (ifOut .ne. 0 .and. (iter > 0 .or.  xx < 0. .or. &
        xx > nxLoad + 1 .or. yy < inLoadStart - 1. .or. &
        yy > inLoadEnd + 1.)) ifDone = 1
    iter = iter + 1
  enddo
  return
end subroutine loadedProjectingPoint
