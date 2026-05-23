! Does back-projection with X-axis tilt using 8 steps at a time; see note in bpsumnox.f90
!
subroutine bpSumXtilt(outSlice, ind1, inputArr, ipBase, ipdel, n, &
    xProj1, cosBeta, yfrac, omyfrac)
  real*4 outSlice(*), inputArr(*)
  real*8 xProj8, xProj1, xProj2, xProj3, xProj4, xProj5, xProj6, xProj7

  n8 = n / 8
  numLeft = n - 8 * n8

  DO j = 1, n8
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
    ip1 = ipBase + iProj1
    ip2 = ip1 + ipdel
    outSlice(ind1) = outSlice(ind1) + &
        (1. -xFrac1) * (omyfrac * inputArr(ip1) + yfrac * inputArr(ip2)) + &
        xFrac1 * (omyfrac * inputArr(ip1 + 1) + yfrac * inputArr(ip2 + 1))
    ind1 = ind1 + 1
    ip1 = ipBase + iProj2
    ip2 = ip1 + ipdel
    outSlice(ind1) = outSlice(ind1) + &
        (1. -xFrac2) * (omyfrac * inputArr(ip1) + yfrac * inputArr(ip2)) + &
        xFrac2 * (omyfrac * inputArr(ip1 + 1) + yfrac * inputArr(ip2 + 1))
    ind1 = ind1 + 1
    ip1 = ipBase + iProj3
    ip2 = ip1 + ipdel
    outSlice(ind1) = outSlice(ind1) + &
        (1. -xFrac3) * (omyfrac * inputArr(ip1) + yfrac * inputArr(ip2)) + &
        xFrac3 * (omyfrac * inputArr(ip1 + 1) + yfrac * inputArr(ip2 + 1))
    ind1 = ind1 + 1
    ip1 = ipBase + iProj4
    ip2 = ip1 + ipdel
    outSlice(ind1) = outSlice(ind1) + &
        (1. -xFrac4) * (omyfrac * inputArr(ip1) + yfrac * inputArr(ip2)) + &
        xFrac4 * (omyfrac * inputArr(ip1 + 1) + yfrac * inputArr(ip2 + 1))
    ind1 = ind1 + 1
    ip1 = ipBase + iProj5
    ip2 = ip1 + ipdel
    outSlice(ind1) = outSlice(ind1) + &
        (1. -xFrac5) * (omyfrac * inputArr(ip1) + yfrac * inputArr(ip2)) + &
        xFrac5 * (omyfrac * inputArr(ip1 + 1) + yfrac * inputArr(ip2 + 1))
    ind1 = ind1 + 1
    ip1 = ipBase + iProj6
    ip2 = ip1 + ipdel
    outSlice(ind1) = outSlice(ind1) + &
        (1. -xFrac6) * (omyfrac * inputArr(ip1) + yfrac * inputArr(ip2)) + &
        xFrac6 * (omyfrac * inputArr(ip1 + 1) + yfrac * inputArr(ip2 + 1))
    ind1 = ind1 + 1
    ip1 = ipBase + iProj7
    ip2 = ip1 + ipdel
    outSlice(ind1) = outSlice(ind1) + &
        (1. -xFrac7) * (omyfrac * inputArr(ip1) + yfrac * inputArr(ip2)) + &
        xFrac7 * (omyfrac * inputArr(ip1 + 1) + yfrac * inputArr(ip2 + 1))
    ind1 = ind1 + 1
    ip1 = ipBase + iProj8
    ip2 = ip1 + ipdel
    outSlice(ind1) = outSlice(ind1) + &
        (1. -xFrac8) * (omyfrac * inputArr(ip1) + yfrac * inputArr(ip2)) + &
        xFrac8 * (omyfrac * inputArr(ip1 + 1) + yfrac * inputArr(ip2 + 1))
    ind1 = ind1 + 1
    xProj1 = xProj8 + cosBeta
  enddo

  do j = 1, numLeft
    iProj1 = xProj1
    xFrac1 = xProj1 - iProj1
    ip1 = ipBase + iProj1
    ip2 = ip1 + ipdel
    outSlice(ind1) = outSlice(ind1) + &
        (1. -xFrac1) * (omyfrac * inputArr(ip1) + yfrac * inputArr(ip2)) + &
        xFrac1 * (omyfrac * inputArr(ip1 + 1) + yfrac * inputArr(ip2 + 1))
    ind1 = ind1 + 1
    xProj1 = xProj1 + cosBeta
  enddo
  return
end subroutine bpSumXtilt
