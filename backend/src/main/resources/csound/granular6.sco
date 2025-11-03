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

;----------------- CRAZIFICATION LEVEL 6 -----------------
i1  0	      REPLACE_ME_DURATION \
    0.25      0.5     2.25   5.5 \        ; iampMin, iampMax, kampCpsMin, kampCpsMax
    0.9       1.1     2.25   5.5 \        ; ipitchMin, ipitchMax, kpitchCpsMin, kpitchCpsMax
    0.7       9.0     2.25   5.5 \        ; idensMin, idensMax, kdensCpsMin, kdensCpsMax
              0.175   2.25   5.5 \        ; kampoffMin(0 fix), kampoffMax, kampoffCpsMin, kampoffCpsMax
    -0.175    0.175   2.25   5.5 \        ; kpitchoffMin, kpitchoffMax, kpitchoffCpsMin, kpitchoffCpsMax
    0.05      0.5     2.25   5.5          ; kgdurMin, kgdurMax, kgdurCpsMin, kgdurCpsMax

    -120.0 	  120.0	  1.0	 5.0		  ; iazLo, iazHi, iazMinCps, iazMaxCps
    -54.00 	  54.0	  1.0	 5.0		  ; ielLo, ielHi, ielMinCps, ielMaxCps

