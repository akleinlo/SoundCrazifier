sr      = 44100
ksmps   = 1
nchnls  = 2
0dbfs   = 1

seed 0

gS_HRTF_left  = "hrtf-44100-left.dat"
gS_HRTF_right = "hrtf-44100-right.dat"

;---------------------------------------------------------
; Granular Instrument – controlled by p-fields
;---------------------------------------------------------

instr 1

;--------------------------------------
; Amplitude
;--------------------------------------
iampMin         =        p4
iampMax         =        p5
kampCpsMin      =        p6
kampCpsMax      =        p7
kamp          rspline    iampMin, iampMax, kampCpsMin, kampCpsMax

;--------------------------------------
; Pitch
;--------------------------------------
insnd           =       1                   ; sample table (from f1 in score)
ibasfrq         =       sr / ftlen(insnd)   ; base frequency for original sample
ipitchMin       =       p8
ipitchMax       =       p9
kpitchCpsMin    =       p10
kpitchCpsMax    =       p11
kpitch        rspline   ibasfrq*ipitchMin, ibasfrq*ipitchMax, kpitchCpsMin, kpitchCpsMax

;--------------------------------------
; Density
;--------------------------------------
idensMin        =       p12
idensMax        =       p13
kdenseCpsMin    =       p14
kdenseCpsMax    =       p15
kdens         rspline   idensMin, idensMax, kdenseCpsMin, kdenseCpsMax

;--------------------------------------
; Amplitude Offset
;--------------------------------------
kampoffMin      =       0
kampoffMax      =       p16
kampoffCpsMin   =       p17
kampoffCpsMax   =       p18
kampoff       rspline   kampoffMin, kampoffMax, kampoffCpsMin, kampoffCpsMax

;--------------------------------------
; Pitch Offset
;--------------------------------------
kpitchoffMin    =       p19
kpitchoffMax    =       p20
kpitchoffCpsMin =       p21
kpitchoffCpsMax =       p22
kpitchoff     rspline   kpitchoffMin, kpitchoffMax, kpitchoffCpsMin, kpitchoffCpsMax

;--------------------------------------
; Grain Duration
;--------------------------------------
kgdurMin        =       p23
kgdurMax        =       p24
kgdurCpsMin     =       p25
kgdurCpsMax     =       p26
kgdur         rspline   kgdurMin, kgdurMax, kgdurCpsMin, kgdurCpsMax

; Security check: Grain length must not be 0
if kgdur <= 0 then
   kgdur = 0.001 ; Minimum 1 ms
endif

;--------------------------------------
; Tables & grain synthesis
;--------------------------------------
giSine        ftgenonce 0, 0, 16385, 10, 1
igfn            =       1                   ; f1 = input sound (set in score)
iwfn            =       giSine
imgdur          =       0.5                 ; max grain duration (seconds)

aout            grain   kamp, kpitch, kdens, kampoff, kpitchoff, kgdur, igfn, iwfn, imgdur

;--------------------------------------
; Binaural 3d processing
;--------------------------------------
; azimuth (direction in the horizontal plane)
iazLo 			= 		p27
iazHi 			= 		p28
iazMinCps 		= 		p29
iazMaxCps 		= 		p30
kAz 		 rspline 	iazLo, iazHi, iazMinCps, iazMaxCps
kAzSmoothed   portk 	kAz, 0.01

; elevation (direction in the vertical plane)
ielLo 			= 		p31
ielHi 			= 		p32
ielMinCps 		= 		p33
ielMaxCps 		= 		p34
kel 		 rspline 	ielLo, ielHi, ielMinCps, ielMaxCps
kelevSmoothed portk 	kel, 0.01


aLeft,aRight hrtfmove2  aout, kAzSmoothed,kelevSmoothed, gS_HRTF_left,gS_HRTF_right

aLeftOut 	linenr 	    aLeft, 0.001, 0, 0.01
aRightOut 	linenr 	    aRight, 0.001, 0, 0.01
aLeft 		balance 	aLeftOut, aLeft
aRight 		balance 	aRightOut, aRight

prints "Using HRTF left: %s\n", gS_HRTF_left
prints "Using HRTF right: %s\n", gS_HRTF_right


outs      	aLeft, aRight
endin
