! Does back-projection with no X-axis tilt using 8 steps at a time.
! This and the other two bpsum routines were originally made so that assembly code could
! be modified to minimize slow switching between rounding modes.
! They are retained for two reasons: 1) the multiple steps run faster than a single loop.
! 2) C translations ran ~50% slower than Fortran with Intel compilers, despite various
! efforts to optimize the code
!
subroutine bpSumNoX(outSlice, ind1, inputArr, ipoint, n, xProj1, cosBeta)
  implicit none
  real*4 outSlice(*), inputArr(*)
  real*8 xProj8, xProj1, xProj2, xProj3, xProj4, xProj5, xProj6, xProj7
  integer*4 ind1, ipoint, n, ipP1, numLeft, n8
  real*4 xFrac1, xFrac2, xFrac3, xFrac4, xFrac5, xFrac6, xFrac7, xFrac8, cosBeta
  integer*4 iProj1, iProj2, iProj3, iProj4, iProj5, iProj6, iProj7, iProj8
  integer*4 ind2, ind3, ind4, ind5, ind6, ind7, ind8,j

  n8 = n / 8
  numLeft = n - 8 * n8
  ipP1 = ipoint + 1

  do j = 1, n8
    xProj2 = xProj1 + cosBeta
    xProj3 = xProj2 + cosBeta
    xProj4 = xProj3 + cosBeta
    xProj5 = xProj4 + cosBeta
    xProj6 = xProj5 + cosBeta
    xProj7 = xProj6 + cosBeta
    xProj8 = xProj7 + cosBeta
    iProj1 = xProj1
    iProj2 = xProj2
    iProj3 = xProj3
    iProj4 = xProj4
    iProj5 = xProj5
    iProj6 = xProj6
    iProj7 = xProj7
    iProj8 = xProj8
    xFrac1 = xProj1 - iProj1
    xFrac2 = xProj2 - iProj2
    xFrac3 = xProj3 - iProj3
    xFrac4 = xProj4 - iProj4
    xFrac5 = xProj5 - iProj5
    xFrac6 = xProj6 - iProj6
    xFrac7 = xProj7 - iProj7
    xFrac8 = xProj8 - iProj8
    ind2 = ind1 + 1
    ind3 = ind2 + 1
    ind4 = ind3 + 1
    ind5 = ind4 + 1
    ind6 = ind5 + 1
    ind7 = ind6 + 1
    ind8 = ind7 + 1
    outSlice(ind1) = outSlice(ind1) + (1. -xFrac1) * inputArr(ipoint + iProj1) &
        + xFrac1 * inputArr(ipP1 + iProj1)
    outSlice(ind2) = outSlice(ind2) + (1. -xFrac2) * inputArr(ipoint + iProj2) &
        + xFrac2 * inputArr(ipP1 + iProj2)
    outSlice(ind3) = outSlice(ind3) + (1. -xFrac3) * inputArr(ipoint + iProj3) &
        + xFrac3 * inputArr(ipP1 + iProj3)
    outSlice(ind4) = outSlice(ind4) + (1. -xFrac4) * inputArr(ipoint + iProj4) &
        + xFrac4 * inputArr(ipP1 + iProj4)
    outSlice(ind5) = outSlice(ind5) + (1. -xFrac5) * inputArr(ipoint + iProj5) &
        + xFrac5 * inputArr(ipP1 + iProj5)
    outSlice(ind6) = outSlice(ind6) + (1. -xFrac6) * inputArr(ipoint + iProj6) &
        + xFrac6 * inputArr(ipP1 + iProj6)
    outSlice(ind7) = outSlice(ind7) + (1. -xFrac7) * inputArr(ipoint + iProj7) &
        + xFrac7 * inputArr(ipP1 + iProj7)
    outSlice(ind8) = outSlice(ind8) + (1. -xFrac8) * inputArr(ipoint + iProj8) &
        + xFrac8 * inputArr(ipP1 + iProj8)
    ind1 = ind8 + 1
    xProj1 = xProj8 + cosBeta
  enddo

  do j = 1, numLeft
    iProj1 = xProj1
    xFrac1 = xProj1 - iProj1
    outSlice(ind1) = outSlice(ind1) + &
        (1. -xFrac1) * inputArr(ipoint + iProj1) &
        + xFrac1 * inputArr(ipP1 + iProj1)
    ind1 = ind1 + 1
    xProj1 = xProj1 + cosBeta
  enddo
  return
end subroutine bpSumNoX
