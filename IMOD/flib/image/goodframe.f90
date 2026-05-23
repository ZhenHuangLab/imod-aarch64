! GOODFRAME reads a number and prints out the next highest valid
! frame size for an FFT.  BY default, it requires no prime factor greater than 19
! regardless of whether FFTW is being used, because blendmont does the same.
! With the option -n, it will use the limit from calling niceFFTlimit instead.
!
! $Id$
!
program goodframe
  implicit none
  integer*4 nices(10)
  character*120 arg
  integer*4 nx, nxp, i, ndo, limit, indFirst
  integer*4 iargc, niceframe, niceFFTlimit
  limit = 19
  indFirst = 1
  if (iargc() == 0) then
    read(5,*) nx
    nx = nx + mod(nx, 2)
    nxp = niceframe(nx, 2, 19)
    print *,nxp
    call exit(0)
  endif
  ndo = min(iargc(), 10)
  call getarg(1, arg)
  if (trim(arg) == '-n') then
    limit = niceFFTlimit();
    indFirst = 2
  endif
  do i = indFirst, ndo
    call getarg(i, arg)
    read(arg, *) nx
    nices(i) = niceframe(nx, 2, limit)
  enddo
  print *,(nices(i), i = indFirst, ndo)
  call exit(0)
end program
