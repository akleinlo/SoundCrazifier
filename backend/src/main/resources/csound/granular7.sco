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


;----------------- CRAZIFICATION LEVEL 7 -----------------
i1  0	      REPLACE_ME_DURATION \
    0.2       0.5     2.5   7.0 \        ; iampMin, iampMax, kampCpsMin, kampCpsMax
    0.8       1.2     2.5   7.0 \        ; ipitchMin, ipitchMax, kpitchCpsMin, kpitchCpsMax
    0.6       12.0    2.5   7.0 \        ; idensMin, idensMax, kdensCpsMin, kdensCpsMax
              0.2     2.5   7.0 \        ; kampoffMin(0 fix), kampoffMax, kampoffCpsMin, kampoffCpsMax
    -0.2      0.2     2.5   7.0 \        ; kpitchoffMin, kpitchoffMax, kpitchoffCpsMin, kpitchoffCpsMax
    0.025     0.45    2.5   7.0          ; kgdurMin, kgdurMax, kgdurCpsMin, kgdurCpsMax

    -150.0 	  150.0	  1.1	7.0			 ; iazLo, iazHi, iazMinCps, iazMaxCps
    -63.00 	  63.0	  1.1	7.0			 ; ielLo, ielHi, ielMinCps, ielMaxCps

