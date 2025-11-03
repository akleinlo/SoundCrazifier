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

;----------------- CRAZIFICATION LEVEL 9 -----------------
i1  0	      REPLACE_ME_DURATION \
    0.05      0.5     3.0   12.0 \        ; iampMin, iampMax, kampCpsMin, kampCpsMax
    0.4       2.0     3.0   12.0 \        ; ipitchMin, ipitchMax, kpitchCpsMin, kpitchCpsMax
    1.0       19.0    3.0   12.0 \        ; idensMin, idensMax, kdensCpsMin, kdensCpsMax
              0.25    3.0   12.0 \        ; kampoffMin(0 fix), kampoffMax, kampoffCpsMin, kampoffCpsMax
    -0.25     0.25    3.0   12.0 \        ; kpitchoffMin, kpitchoffMax, kpitchoffCpsMin, kpitchoffCpsMax
    0.005     0.35    3.0   12.0          ; kgdurMin, kgdurMax, kgdurCpsMin, kgdurCpsMax

    -210.00   210.0	  1.3	12.5	      ; iazLo, iazHi, iazMinCps, iazMaxCps
    -81.00 	  81.0	  1.3	12.5		  ; ielLo, ielHi, ielMinCps, ielMaxCps

