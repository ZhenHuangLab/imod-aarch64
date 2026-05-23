! Module 
! Make sure FWRAP_MAX_OBJECT matches max_obj_num and FWRAP_MAX_POINTS matches
! max_pt in imodel_fwrap.c; these would be a large-model default for any 
!
module fortmodel
  implicit none
  integer max_clabel, maxTypes
  parameter (max_clabel = 200)                !max # of text labels
  parameter (maxTypes = 2)

  integer*4 max_obj_num   /1000000/
  integer*4 max_pt       /20000000/
  integer*4 len_object   /24000000/
  integer*4 max_obj_order /1200000/
  logical*4 fmInitialized /.false./;
  integer*4 fmMaxObjNumDflt(maxTypes) /1000000, 100000/
  integer*4 fmMaxPtNumDflt(maxTypes)  /20000000, 1000000/
  protected max_obj_num, max_pt, len_object, max_obj_order
  private fmInitialized, fmMaxObjNumDflt, fmMaxPtNumDflt
  !
  ! Variables to be set by calling program or library routines
  ! fmModSizeType is 1 for standard size, 2 for small model.  The model will always be at
  ! least these default sizes
  ! fmNeedObjects and fmNeedPoints are the minimum number of objects and points needed
  ! These are the only values considered here
  ! fmIncReadObjBy and fmIncReadPointsBy are the minimum number of additional objects
  ! and points above the number read in, to be set by a caller
  ! fmMaxObjLoaded are the maximum number of objects to be loaded, or 0 if all
  ! fmBoostReadInBy is the factor by which to increase # number of objects/points read in
  integer*4 fmModSizeType /1/
  integer*4 fmNeedObjects /0/, fmNeedPoints /0/
  integer*4 fmIncReadObjBy /0/, fmIncReadPointsBy /0/
  integer*4 fmMaxObjLoaded /0/
  real*4 fmBoostReadInBy /1.5/

  
  integer*4, allocatable :: object(:)       ! pointer to p_coord array (len_object)
  integer*4, allocatable :: npt_in_obj(:)   ! # of points in object (max_obj_num)
  integer*4, allocatable :: ibase_obj(:)    ! base index of object in OBJECT (max_obj_num)
  integer*4, allocatable :: obj_color(:,:)  ! ON/OFF and color indexes (max_obj_num)
  integer*4, allocatable :: obj_order(:)    ! list of objects in order (max_obj_order)
  integer*4, allocatable :: ndx_order(:)    ! index of object's entry in OBJ_ORDER
  real*4, allocatable :: p_coord(:,:)       ! model coordinates of points (max_pt)
  integer*1, allocatable :: pt_label(:)     ! symbol number for point (max_pt)
  character*10 clabel(max_clabel)           ! text labels
  integer*4 label_list(max_clabel)          ! list of points with labels
  integer*4 n_point                         ! highest point # in p_coord
  integer*4 n_object                        ! total # of non-zero objects
  integer*4 ibase_free                      ! base index of free area in OBJECT
  integer*4 ntot_in_obj                     ! total entries in OBJECT
  integer*4 nin_order                       ! # of entries in OBJ_ORDER
  integer*4 max_mod_obj                     ! highest object # used so far
  integer*4 n_clabel                        ! number of text labels

CONTAINS
  ! Computes the needed size for object and point arrays from defaults or caller's
  ! values and alloctes or reallocates as necessary
  ! Also calls imodArrayLimits to inform libimod of these limits
  !
  subroutine allocateFortModel    
    implicit none
    integer*4 ierr
    if (fmInitialized) then
      deallocate(object, npt_in_obj, ibase_obj, obj_color, obj_order, ndx_order, &
          p_coord, pt_label, stat = ierr)
    endif
    fmModSizeType = max(1, min(2, fmModSizeType))
    max_obj_num = max(fmMaxObjNumDflt(fmModSizeType), fmNeedObjects)
    max_pt = max(fmMaxPtNumDflt(fmModSizeType), fmNeedPoints)
    len_object = max_pt + max_pt / 5     !len of pointer array "object"
    max_obj_order = max_obj_num + max_obj_num / 5
    allocate(object(len_object), npt_in_obj(max_obj_num), ibase_obj(max_obj_num), &
        obj_color(2, max_obj_num), obj_order(max_obj_order), ndx_order(max_obj_num), &
        p_coord(3, max_pt), pt_label(max_pt), stat = ierr)
    call memoryError(ierr, 'arrays for model storage in Fortran')
    fmInitialized = .true.
    call imodArrayLimits(max_pt, max_obj_num)
    return
  end subroutine allocateFortModel
end module fortmodel
