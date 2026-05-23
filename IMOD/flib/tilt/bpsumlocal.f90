! Does back-projection with local alignment information using5 steps at a time; see note 
! in bpsumnox.f90
!
subroutine bpSumLocal(outSlice, index, inputArr, zz, xProjF, xProjZ, yProjF, &
    yProjZ, ipoint, ipDel, lslice, jStart, jEnd, nprojSuper)
  implicit none
  real*4 outSlice(*), inputArr(*), xProjF(*), xProjZ(*), yProjF(*), yProjZ(*), zz
  integer*4 index, ipoint, ipDel, lslice, jStart, jEnd, nprojSuper
  integer*4 numDiv, numFast, jEndFast, jStartSlow, j, k, ip1, ip2, iProj, jProj
  integer*4 iProj1, jProj1, iProj2, jProj2, iProj3, jProj3, iProj4, jProj4, iProj5, jProj5
  real*4 xProj1, yProj1, xProj2, yProj2, xProj3, yProj3, xProj4, yProj4, xProj5, yProj5
  real*4 xProj, yProj, xFrac, yFrac

  numDiv = 5
  numFast = (jEnd + 1 - jStart) / numDiv
  jEndFast = jStart + (numFast - 1) * numDiv
  jStartSlow = jStart + numFast * numDiv

  ! It took 20% longer to do the loop with the proj factor of 1, so duplicate it
  if (nprojSuper == 1) then
    do j = jStart, jEndFast, numDiv
      k = j
      xProj1 = xProjF(k) + zz * xProjZ(k)
      yProj1 = yProjF(k) + zz * yProjZ(k)
      k = k + 1
      xProj2 = xProjF(k) + zz * xProjZ(k)
      yProj2 = yProjF(k) + zz * yProjZ(k)
      k = k + 1
      xProj3 = xProjF(k) + zz * xProjZ(k)
      yProj3 = yProjF(k) + zz * yProjZ(k)
      k = k + 1
      xProj4 = xProjF(k) + zz * xProjZ(k)
      yProj4 = yProjF(k) + zz * yProjZ(k)
      k = k + 1
      xProj5 = xProjF(k) + zz * xProjZ(k)
      yProj5 = yProjF(k) + zz * yProjZ(k)
      iProj1 = xProj1
      jProj1 = yProj1
      iProj2 = xProj2
      jProj2 = yProj2
      iProj3 = xProj3
      jProj3 = yProj3
      iProj4 = xProj4
      jProj4 = yProj4
      iProj5 = xProj5
      jProj5 = yProj5
      !
      xFrac = xProj1 - iProj1
      yFrac = yProj1 - jProj1
      ip1 = ipoint + (jProj1 - lslice) * ipDel + iProj1
      ip2 = ip1 + ipDel
      outSlice(index) = outSlice(index) + &
          (1. -yFrac) * ((1. -xFrac) * inputArr(ip1) + xFrac * inputArr(ip1 + 1)) + &
          yFrac * ((1. -xFrac) * inputArr(ip2) + xFrac * inputArr(ip2 + 1))
      ! INDEX=INDEX+1
      !
      xFrac = xProj2 - iProj2
      yFrac = yProj2 - jProj2
      ip1 = ipoint + (jProj2 - lslice) * ipDel + iProj2
      ip2 = ip1 + ipDel
      outSlice(index + 1) = outSlice(index + 1) + &
          (1. -yFrac) * ((1. -xFrac) * inputArr(ip1) + xFrac * inputArr(ip1 + 1)) + &
          yFrac * ((1. -xFrac) * inputArr(ip2) + xFrac * inputArr(ip2 + 1))
      index = index + 2
      !
      xFrac = xProj3 - iProj3
      yFrac = yProj3 - jProj3
      ip1 = ipoint + (jProj3 - lslice) * ipDel + iProj3
      ip2 = ip1 + ipDel
      outSlice(index) = outSlice(index) + &
          (1. -yFrac) * ((1. -xFrac) * inputArr(ip1) + xFrac * inputArr(ip1 + 1)) + &
          yFrac * ((1. -xFrac) * inputArr(ip2) + xFrac * inputArr(ip2 + 1))
      index = index + 1
      !
      xFrac = xProj4 - iProj4
      yFrac = yProj4 - jProj4
      ip1 = ipoint + (jProj4 - lslice) * ipDel + iProj4
      ip2 = ip1 + ipDel
      outSlice(index) = outSlice(index) + &
          (1. -yFrac) * ((1. -xFrac) * inputArr(ip1) + xFrac * inputArr(ip1 + 1)) + &
          yFrac * ((1. -xFrac) * inputArr(ip2) + xFrac * inputArr(ip2 + 1))
      index = index + 1
      !
      xFrac = xProj5 - iProj5
      yFrac = yProj5 - jProj5
      ip1 = ipoint + (jProj5 - lslice) * ipDel + iProj5
      ip2 = ip1 + ipDel
      outSlice(index) = outSlice(index) + &
          (1. -yFrac) * ((1. -xFrac) * inputArr(ip1) + xFrac * inputArr(ip1 + 1)) + &
          yFrac * ((1. -xFrac) * inputArr(ip2) + xFrac * inputArr(ip2 + 1))
      index = index + 1
    enddo

  else
    do j = jStart, jEndFast, numDiv
      k = j
      xProj1 = nprojSuper * (xProjF(k) + zz * xProjZ(k) - 0.5) + 0.5
      yProj1 = yProjF(k) + zz * yProjZ(k)
      k = k + 1
      xProj2 = nprojSuper * (xProjF(k) + zz * xProjZ(k) - 0.5) + 0.5
      yProj2 = yProjF(k) + zz * yProjZ(k)
      k = k + 1
      xProj3 = nprojSuper * (xProjF(k) + zz * xProjZ(k) - 0.5) + 0.5
      yProj3 = yProjF(k) + zz * yProjZ(k)
      k = k + 1
      xProj4 = nprojSuper * (xProjF(k) + zz * xProjZ(k) - 0.5) + 0.5
      yProj4 = yProjF(k) + zz * yProjZ(k)
      k = k + 1
      xProj5 = nprojSuper * (xProjF(k) + zz * xProjZ(k) - 0.5) + 0.5
      yProj5 = yProjF(k) + zz * yProjZ(k)
      iProj1 = xProj1
      jProj1 = yProj1
      iProj2 = xProj2
      jProj2 = yProj2
      iProj3 = xProj3
      jProj3 = yProj3
      iProj4 = xProj4
      jProj4 = yProj4
      iProj5 = xProj5
      jProj5 = yProj5
      !
      xFrac = xProj1 - iProj1
      yFrac = yProj1 - jProj1
      ip1 = ipoint + (jProj1 - lslice) * ipDel + iProj1
      ip2 = ip1 + ipDel
      outSlice(index) = outSlice(index) + &
          (1. -yFrac) * ((1. -xFrac) * inputArr(ip1) + xFrac * inputArr(ip1 + 1)) + &
          yFrac * ((1. -xFrac) * inputArr(ip2) + xFrac * inputArr(ip2 + 1))
      ! INDEX=INDEX+1
      !
      xFrac = xProj2 - iProj2
      yFrac = yProj2 - jProj2
      ip1 = ipoint + (jProj2 - lslice) * ipDel + iProj2
      ip2 = ip1 + ipDel
      outSlice(index + 1) = outSlice(index + 1) + &
          (1. -yFrac) * ((1. -xFrac) * inputArr(ip1) + xFrac * inputArr(ip1 + 1)) + &
          yFrac * ((1. -xFrac) * inputArr(ip2) + xFrac * inputArr(ip2 + 1))
      index = index + 2
      !
      xFrac = xProj3 - iProj3
      yFrac = yProj3 - jProj3
      ip1 = ipoint + (jProj3 - lslice) * ipDel + iProj3
      ip2 = ip1 + ipDel
      outSlice(index) = outSlice(index) + &
          (1. -yFrac) * ((1. -xFrac) * inputArr(ip1) + xFrac * inputArr(ip1 + 1)) + &
          yFrac * ((1. -xFrac) * inputArr(ip2) + xFrac * inputArr(ip2 + 1))
      index = index + 1
      !
      xFrac = xProj4 - iProj4
      yFrac = yProj4 - jProj4
      ip1 = ipoint + (jProj4 - lslice) * ipDel + iProj4
      ip2 = ip1 + ipDel
      outSlice(index) = outSlice(index) + &
          (1. -yFrac) * ((1. -xFrac) * inputArr(ip1) + xFrac * inputArr(ip1 + 1)) + &
          yFrac * ((1. -xFrac) * inputArr(ip2) + xFrac * inputArr(ip2 + 1))
      index = index + 1
      !
      xFrac = xProj5 - iProj5
      yFrac = yProj5 - jProj5
      ip1 = ipoint + (jProj5 - lslice) * ipDel + iProj5
      ip2 = ip1 + ipDel
      outSlice(index) = outSlice(index) + &
          (1. -yFrac) * ((1. -xFrac) * inputArr(ip1) + xFrac * inputArr(ip1 + 1)) + &
          yFrac * ((1. -xFrac) * inputArr(ip2) + xFrac * inputArr(ip2 + 1))
      index = index + 1
    enddo
  endif

  do j = jStartSlow, jEnd
    xProj = nprojSuper * (xProjF(j) + zz * xProjZ(j) - 0.5) + 0.5
    yProj = yProjF(j) + zz * yProjZ(j)
    iProj = xProj
    xFrac = xProj - iProj
    jProj = yProj
    yFrac = yProj - jProj
    !
    ip1 = ipoint + (jProj - lslice) * ipDel + iProj
    ip2 = ip1 + ipDel
    outSlice(index) = outSlice(index) + &
        (1. -yFrac) * ((1. -xFrac) * inputArr(ip1) + xFrac * inputArr(ip1 + 1)) + &
        yFrac * ((1. -xFrac) * inputArr(ip2) + xFrac * inputArr(ip2 + 1))
    index = index + 1
  enddo
  return
end subroutine bpSumLocal
