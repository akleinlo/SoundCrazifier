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

;----------------- CRAZIFICATION LEVEL 8 -----------------
i1  0	      REPLACE_ME_DURATION \
    0.1       0.5     2.75   9.0 \        ; iampMin, iampMax, kampCpsMin, kampCpsMax
    0.6       1.4     2.75   9.0 \        ; ipitchMin, ipitchMax, kpitchCpsMin, kpitchCpsMax
    0.5       15.0    2.75   9.0 \        ; idensMin, idensMax, kdensCpsMin, kdensCpsMax
              0.225   2.75   9.0 \        ; kampoffMin(0 fix), kampoffMax, kampoffCpsMin, kampoffCpsMax
    -0.225    0.225   2.75   9.0 \        ; kpitchoffMin, kpitchoffMax, kpitchoffCpsMin, kpitchoffCpsMax
    0.01      0.4     2.75   9.0          ; kgdurMin, kgdurMax, kgdurCpsMin, kgdurCpsMax

    -180.0 	  180.0	  1.2	 9.5		  ; iazLo, iazHi, iazMinCps, iazMaxCps
    -72.00 	  72.0	  1.2	 9.5		  ; ielLo, ielHi, ielMinCps, ielMaxCps

