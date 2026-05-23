!  $Id$
!
! SCALE takes the minimum and maximum data values and finds "nice"
! axis limits that contain them, as specified by the lower limit XLO
! and an increment DX, 1/10 of the axis length.
!
subroutine scale(xmin, xmax, dx, xlo)
  implicit none
  real*4 goodRange(10), xmin, xmax, dx, xlo
  data goodRange/1., 1.5, 2., 2.5, 3., 4., 5., 6., 8., 10./
  integer*4 indRange, numGoodDivs(10)
  data numGoodDivs/10, 10, 10, 10, 10, 10, 10, 10, 10, 10/
  call scaleCommon(xmin, xmax, goodRange, numGoodDivs, 10, dx, xlo, indRange)
  return
end subroutine scale


! scaleMultiDiv also takes the minimum and maximum data values and finds "nice"
! axis limits that contain them, as specified by the lower limit XLO and an increment
! DX, 1/10 of the axis length.  However, it provides one more data range, 1.2, and it
! returns an optimal number of axis divisions in numDiv, which can be 8, 10, or 12, and
! the size of those divisions in dxDiv.
!
subroutine scaleMultiDiv(xmin, xmax, dx, xlo, numDiv, dxDiv)
  implicit none
  real*4 goodRange(11), xmin, xmax, dx, xlo, dxDiv
  data goodRange/1., 1.2, 1.5, 2., 2.5, 3., 4., 5., 6., 8., 10./
  integer*4 numGoodDivs(11), indRange, numDiv
  data numGoodDivs/10, 12, 10, 10, 10, 10, 8, 10, 12, 8, 10/
  call scaleCommon(xmin, xmax, goodRange, numGoodDivs, 11, dx, xlo, indRange)
  numDiv = numGoodDivs(indRange)
  dxDiv = (10. * dx) / numDiv
  return
end subroutine scaleMultiDiv


! The common scale routine takes the list of good data ranges and returns the index of
! the selected range
subroutine scaleCommon(xmin, xmax, goodRange, numGoodDivs, numRange, dx, xlo, indRange)
  implicit none
  real*4 goodRange(*), xmin, xmax, dx, xlo
  integer*4 numGoodDivs(*), numRange, indRange, loop, logExp, ndivs
  real*4 xloLast, dxLast, dxLog, fract, dxDiv
  xlo = xmin
  xloLast = 1.e30
  dxLast = 1.e30
  do loop = 1, 100
    !
    ! If there is single point or no range, set range so point does not end up on an axis
    if (xmax <= xlo) then
      xlo = xlo - 0.5
      xmax = xlo + 1.
    endif
    dxLog = alog10(xmax - xlo)
    logExp = dxLog
    if (dxLog < 0.) logExp = logExp - 1
    fract = (xmax - xlo) / 10.**(logExp)
    do indRange = 1, numRange
      if (fract <= goodRange(indRange)) then
        exit
      endif
    enddo
    !
    ! Callers expect dx to still be based on 10 divisions, but do the refinements
    ! based on the actual number of divisions
    ndivs = numGoodDivs(indRange)
    dx = goodRange(indRange) * 10.**(logExp - 1)
    dxDiv = 10. * dx / ndivs
    xlo = dxDiv * ifix(xmin / dxDiv)
    if (xlo > xmin) xlo = xlo - dxDiv
    if (xlo == xmin .and. xlo + (ndivs - 1.) * dxDiv > xmax) xlo = xlo - dxDiv
    if ((xlo == xloLast .and. dx == dxLast) .or. xlo + ndivs * dxDiv >= xmax) then
      do while (xlo >= dxDiv .and. xlo + (ndivs - 1.) * dxDiv > xmax)
        xlo = xlo - dxDiv
      enddo
      return
    endif
    xloLast = xlo
    dxLast = dx
  enddo
  return
end subroutine scaleCommon
