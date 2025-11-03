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

;----------------- CRAZIFICATION LEVEL 4 -----------------
i1  0	      REPLACE_ME_DURATION \
    0.35      0.5     1.75   3.5 \        ; iampMin, iampMax, kampCpsMin, kampCpsMax
    0.95      1.05    1.75   3.5 \        ; ipitchMin, ipitchMax, kpitchCpsMin, kpitchCpsMax
    1.0       5.0     1.75   3.5 \        ; idensMin, idensMax, kdensCpsMin, kdensCpsMax
              0.125   1.75   3.5 \        ; kampoffMin(0 fix), kampoffMax, kampoffCpsMin, kampoffCpsMax
    -0.125    0.125   1.75   3.5 \        ; kpitchoffMin, kpitchoffMax, kpitchoffCpsMin, kpitchoffCpsMax
    0.2       0.5     1.75   3.5          ; kgdurMin, kgdurMax, kgdurCpsMin, kgdurCpsMax

    -80.00 	  80.0	  0.8	 3.0		  ; iazLo, iazHi, iazMinCps, iazMaxCps
    -36.00 	  36.0	  0.8	 3.0		  ; ielLo, ielHi, ielMinCps, ielMaxCps

