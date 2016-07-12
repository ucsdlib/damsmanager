#!/bin/sh

input=$1
folder=$2
if [ ! "$folder" ]; then
	folder="netpbm"
fi

TILE=256

#Step 0.
#Exit if the input file doesn't exist
if [ ! -f $input ]; then
	echo "Unable to find $input!"
	exit 1
fi

#Step 1.
#Scale the image until both the x and y dimensions are less than 257

#First convert the input file into a pnmfile

ext=`echo $input | awk -F "." '{print $NF}' | tr 'A-Z' 'a-z'`
folder=$folder/`basename $input | sed 's/\.[^.]*$//'`
if [ $ext = "gif" ]; then
	giftopnm -quiet $input > 0.pnm 2>/dev/null
elif [ $ext = "jpg" -o $ext = "jpeg" ]; then
	djpeg -pnm $input > 0.pnm
elif [ $ext = "tif" -o $ext = "tiff" ]; then
	tifftopnm -quiet $input > 0.pnm 2>/dev/null
elif [ $ext = "png" ]; then
	pngtopnm $input > 0.pnm
elif [ $ext = "cr2" -o $ext = "nef" ]; then
	# decode raw images with dcraw
	dcraw -c -w $input > 0.pnm
else
	echo "Unknown extension: $ext"
	exit 1
fi

Xinitial=`pnmfile 0.pnm | awk -F raw, '{print $2}' | awk '{print $1}'` ; 
Yinitial=`pnmfile 0.pnm | awk -F raw, '{print $2}' | awk '{print $3}'` ;

# round dimensions to multiple of $TILE, if needed
Xround=$TILE
Yround=$TILE
while [ $Xround -lt $Xinitial ]; do
	Xround=$(( $Xround * 2 ))
done
while [ $Yround -lt $Yinitial ]; do
	Yround=$(( $Yround * 2 ))
done
if [ $Xround != $Xinitial -o $Yround != $Yinitial ]; then
	pnmpad -width $Xround -height $Yround 0.pnm > a.pnm
	mv a.pnm 0.pnm
fi

##echo "Input File size = $Xinitial x $Yinitial"

inputfile=0
destfile=1
continue=1

while test $continue -eq 1; do
	pnmscalefixed .5 $inputfile.pnm >$destfile.pnm
        if [ $? -ne 0 ]; then
          exit 1
        fi
	x=`pnmfile $destfile.pnm | awk -F raw, '{print $2}' | awk '{print $1}'` ; 
	y=`pnmfile $destfile.pnm | awk -F raw, '{print $2}' | awk '{print $3}'` ; 
    #echo $x"x"$y
	if [ $x -le $TILE -a $y -le $TILE ]; then
		continue=0;
	fi
	inputfile=$destfile
	let destfile=$destfile+1
done

mkdir -p "$folder/TileGroup0"
if [ $? -ne 0 ]; then
	exit 1
fi

# move the smallest file over

cjpeg $inputfile.pnm >$folder/TileGroup0/0-0-0.jpg

rm $inputfile.pnm

let inputfile=$inputfile-1
Z=1
Tiles=1
TileGroup=0
InGroup=1
while test $inputfile -ge 0 ; do
	x=`pnmfile $inputfile.pnm | awk -F raw, '{print $2}' | awk '{print $1}'` ;
	y=`pnmfile $inputfile.pnm | awk -F raw, '{print $2}' | awk '{print $3}'` ;

	let cols=$x/$TILE
	let rows=$y/$TILE
	let xleft=$x-$cols*$TILE
	let yleft=$y-$rows*$TILE
	
	##echo "$inputfile: ${cols}x${rows} / ${xleft}x${yleft}"
	
	#Rows
	
	Row=0
	while test $Row -lt $rows ; do
		#Strip out the row first to use it.

		posx=0
		let posy=$Row*$TILE
		pnmcut $posx $posy $x $TILE $inputfile.pnm > Therow.pnm
		Col=0
		while test $Col -lt $cols ; do
			let posx=$Col*$TILE
			let posy=$Row*$TILE
			#echo "$Row, $Col -> $posx, $posy, $TILE, $TILE"
			pnmcut $posx 0 $TILE $TILE Therow.pnm | cjpeg >$folder/TileGroup$TileGroup/$Z-$Col-$Row.jpg
			let Tiles=$Tiles+1
			#/bin/echo -n "."
			let InGroup=$InGroup+1
			if test $InGroup -ge $TILE ; then
				let TileGroup=$TileGroup+1
				InGroup=0
				mkdir $folder/TileGroup$TileGroup
			fi
			let Col=$Col+1
		done
		if test $xleft -gt 0 ; then
			let posx=$Col*$TILE
			let posy=$Row*$TILE
			#echo "$Row, $Col -> $posx, $posy, $xleft, $TILE"
			pnmcut $posx 0 $xleft $TILE Therow.pnm | cjpeg >$folder/TileGroup$TileGroup/$Z-$Col-$Row.jpg
			#/bin/echo -n "."
			let Tiles=$Tiles+1
			let InGroup=$InGroup+1
			if test $InGroup -ge $TILE ; then
				let TileGroup=$TileGroup+1
				InGroup=0
				mkdir $folder/TileGroup$TileGroup
			fi
		fi
		let Row=$Row+1
		rm Therow.pnm
		#echo
	done
	if test $yleft -gt 0 ; then
		Col=0
		posx=0
		let posy=$Row*$TILE
		pnmcut $posx $posy $x $yleft $inputfile.pnm > Therow.pnm
		while test $Col -lt $cols ; do
			let posx=$Col*$TILE
			let posy=$Row*$TILE
			#echo "$Row, $Col -> $posx, $posy, $TILE, $yleft"
			pnmcut $posx 0 $TILE $yleft Therow.pnm | cjpeg >$folder/TileGroup$TileGroup/$Z-$Col-$Row.jpg
			#/bin/echo -n "."
			let Tiles=$Tiles+1
			let InGroup=$InGroup+1
			if test $InGroup -ge $TILE ; then
				let TileGroup=$TileGroup+1
				InGroup=0
				mkdir $folder/TileGroup$TileGroup
			fi
			let Col=$Col+1
		done
		if test $xleft -gt 0 ; then
			let posx=$Col*$TILE
			let posy=$Row*$TILE
			#echo "$Row, $Col -> $posx, $posy, $xleft, $yleft"
			pnmcut $posx 0 $xleft $yleft Therow.pnm | cjpeg >$folder/TileGroup$TileGroup/$Z-$Col-$Row.jpg
			#/bin/echo -n "."
			let Tiles=$Tiles+1
			let InGroup=$InGroup+1
			if test $InGroup -ge $TILE ; then
				let TileGroup=$TileGroup+1
				InGroup=0
				mkdir $folder/TileGroup$TileGroup
			fi
		fi
		rm Therow.pnm
	fi
	let Z=$Z+1
	rm $inputfile.pnm
	let inputfile=$inputfile-1
	#echo
done

##echo "Tiles created = $Tiles"

echo "<IMAGE_PROPERTIES WIDTH=\"$Xround\" HEIGHT=\"$Yround\" NUMTILES=\"$Tiles\" NUMIMAGES=\"1\" VERSION=\"1.8\" TILESIZE=\"$TILE\" />" >$folder/ImageProperties.xml
