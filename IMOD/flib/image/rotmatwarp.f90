! rotmatwarp.f  - contains a common module for Rotatevol, Matchvol, and
! Warpvol
!
! $Id$
!
module rotmatwarp
  implicit none
  integer LMCUBE
  parameter (LMCUBE = 2500)
  integer*4 inputDim(3), idimOut(3)
  logical*4 needScratch(4), chunkedHDF/.false./
  integer*4 memoryLim/4096/
  integer*4 nxChunk/-1/, nyChunk/-1/, nzChunk/-1/
  real*8 arraySize8
  integer*4 iVerbose/0/
  integer*4 maxZout/64/
  !
  integer*4 nxyzIn(3), nxyzOut(3), nxyzChunk(3)
  integer*4 nxIn, nyIn, nzin, nxOut, nyOut, nzOut
  real*4 cxyzIn(3), cxyzOut(3)
  real*4 cxIn, cyIn, czIn, cxOut, cyOut, czOut
  equivalence (nxyzIn(1), nxIn), (nxyzIn(2), nyIn), (nxyzIn(3), nzin)
  equivalence (nxyzOut(1), nxOut), (nxyzOut(2), nyOut), (nxyzOut(3), nzOut)
  equivalence (cxyzIn(1), cxIn), (cxyzIn(2), cyIn), (cxyzIn(3), czIn)
  equivalence (cxyzOut(1), cxOut), (cxyzOut(2), cyOut), (cxyzOut(3), czOut)
  equivalence (nxyzChunk(1), nxChunk), (nxyzChunk(2), nyChunk), (nxyzChunk(3), nzChunk)
  !
  real*4 aInv(3,3), title(20), dmeanIn
  integer*4 nCubes(3), mode, ioutXaxis, ioutYaxis, ioutZaxis, limInner, limOuter
  integer*4 idirXaxis, nxyzCube(3,LMCUBE), ixyzCube(3,LMCUBE)
  integer*4, allocatable :: izInFile(:,:,:)
  integer*2, allocatable :: ifile(:,:)
  real*4, allocatable :: array(:,:,:), brray(:)
end module rotmatwarp

