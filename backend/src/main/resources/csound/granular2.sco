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


;----------------- CRAZIFICATION LEVEL 2 -----------------
i1  0	      REPLACE_ME_DURATION \
    0.45      0.55     1.25   2.5 \        ; iampMin, iampMax, kampCpsMin, kampCpsMax
    0.99      1.01     1.25   2.5  \       ; ipitchMin, ipitchMax, kpitchCpsMin, kpitchCpsMax
    1.5       2.5      1.25   2.5  \       ; idensMin, idensMax, kdensCpsMin, kdensCpsMax
              0.075    1.25   2.5  \       ; kampoffMin(0 fix), kampoffMax, kampoffCpsMin, kampoffCpsMax
    -0.075    0.075    1.25   2.5  \       ; kpitchoffMin, kpitchoffMax, kpitchoffCpsMin, kpitchoffCpsMax
    0.4       0.5      1.25   2.5          ; kgdurMin, kgdurMax, kgdurCpsMin, kgdurCpsMax

    -40.00    40.0	   0.6	  2.0		   ; iazLo, iazHi, iazMinCps, iazMaxCps
    -18.00 	  18.0	   0.6	  2.0		   ; ielLo, ielHi, ielMinCps, ielMaxCps

