#!/usr/bin/env zsh

readonly PATH="../data/combined/"
readonly CARD=$PATH"Card_s57g01ljmpgc0titt2okdosoht.db"
readonly READER=$PATH"Reader_krdjav292jj1phl6jjqu0fq77v.db"
readonly RELAY=$PATH"Relay_Card_bdrfge309idmlebo8urlq86rs6.db"
readonly RESULTS="./results"

/usr/bin/python analyser.py $CARD $READER $RESULTS &
/usr/bin/python analyser_corr.py $CARD $READER $RESULTS &
