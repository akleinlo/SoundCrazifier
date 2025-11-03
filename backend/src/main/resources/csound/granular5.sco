;=============================================
; Test Score for granular.orc
;=============================================

; f1 = Audiofile (Mono or Stereo WAV)
f1 0 0 1 "REPLACE_ME" 0 0 0

; optional: Sine-Wave-Test (not necessary, as it is generated in the .orc file.)
; f2 0 8192 10 1

;---------------------------------------------
; i1=instrument, p2=start time, p3=duration
; p4–p34 are parameters that you can be varied
;---------------------------------------------



;----------------- CRAZIFICATION LEVEL 5 -----------------
i1  0	      REPLACE_ME_DURATION \
    0.3       0.5     2.0   4.0 \        ; iampMin, iampMax, kampCpsMin, kampCpsMax
    0.925     1.075   2.0   4.0 \        ; ipitchMin, ipitchMax, kpitchCpsMin, kpitchCpsMax
    0.8       7.0     2.0   4.0 \        ; idensMin, idensMax, kdensCpsMin, kdensCpsMax
              0.15    2.0   4.0 \        ; kampoffMin(0 fix), kampoffMax, kampoffCpsMin, kampoffCpsMax
    -0.15     0.15    2.0   4.0 \        ; kpitchoffMin, kpitchoffMax, kpitchoffCpsMin, kpitchoffCpsMax
    0.1       0.5     2.0   4.0          ; kgdurMin, kgdurMax, kgdurCpsMin, kgdurCpsMax

    -100.0 	  100.0	  0.9	3.5			 ; iazLo, iazHi, iazMinCps, iazMaxCps
    -45.00 	  45.0	  0.9	3.5			 ; ielLo, ielHi, ielMinCps, ielMaxCps

