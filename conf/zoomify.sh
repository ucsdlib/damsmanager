#!/bin/sh

# zoomify-queue.sh - list master image files available in dams4 repo and
#                    generate zoomify tilesets for them, if not already present

#need this to find ./zoomify-netpbm.sh
cd `dirname $0`

if [ ! "$DAMS_BASE" ]; then
	DAMS_BASE=$2
##        DAMS_BASE="http://lib-hydratail-prod.ucsd.edu:8080/dams"

fi
if [ ! "$ZOOM_DIR" ]; then
	ZOOM_DIR="/pub/data1/zoomify"
fi

if [ ! "$LOCAL_STORE" ]; then
    if [ -n "$3" ]; then
        echo "Setting filestore $3"
        LOCAL_STORE=$3
    else
	LOCAL_STORE=/pub/data2/dams/localStore
    fi
fi

varrun=varrun
if [ -f $varrun ]; then
  echo "already running"
  cat $varrun
  exit 1
fi
date >> $varrun
echo "pid = $$" >> $varrun
trap "rm -rf $varrun; exit" EXIT SIGHUP SIGINT SIGTERM

if [ "$1" -a -f "$1" ]; then
	echo "Regenerating files listed in $1"
	TODO="$1"
else
	# list files
##	echo "Listing files..."
##	curl -o tmp.1 $DAMS_BASE/api/files/image-source?format=properties
##	grep "files\." tmp.1 | cut -d= -f2 | tr -d '\\' | sed -e's/.*20775\///' > tmp.2
##	TODO=tmp.2

        TODO=`echo $1 | cut -d= -f2 | sed -e's/.*20775\///'`
        echo "Creating titles for file $TODO ..."
fi

# for each file, check if zoomify dir exists
# if not
#   retrieve file from REST API
#   netpbm.sh file
#   mv dir to zoomify dir
# $ZOOM_DIR/bb/00/bb00012396
PROCESSED=0
##SKIPPED=0
ERRORS=0
##for i in `cat $TODO`; do

	# parse ark/cmp/file
	ARK=`echo $TODO | cut -d/ -f1`
	EXT=`echo $TODO | cut -d. -f2`
	ext=`echo "$EXT" | tr [A-Z] [a-z]`
	FILE=`echo $TODO | cut -d/ -f3`
	if [ "$FILE" ]; then
		CMP=`echo $TODO | cut -d/ -f2`
		DIRNAME=$ARK-$CMP
		FN=20775-$ARK-$CMP-$FILE
	else
		DIRNAME=$ARK
		FILE=`echo $TODO | cut -d/ -f2`
		FN=20775-$ARK-0-$FILE
	fi
	P1=`echo $ARK | cut -c1-2`
	P2=`echo $ARK | cut -c3-4`
	P3=`echo $ARK | cut -c5-6`
	P4=`echo $ARK | cut -c7-8`
	P5=`echo $ARK | cut -c9-10`
	PP=$P1/$P2/$P3/$P4/$P5

	# check if dir already exists
##	if [ -d $ZOOM_DIR/$P1/$P2/$DIRNAME ]; then
##		SKIPPED=$(( $SKIPPED + 1 ))
##		if [ $(( $SKIPPED % 100 )) == 0 ]; then
##			echo $SKIPPED already exist, $ERRORS errors
##		fi
##	elif [ $ext = "cr2" -o $ext = "gif" -o $ext = "jpg" -o $ext = "jpeg" \

        if [ $ext = "cr2" -o $ext = "gif" -o $ext = "jpg" -o $ext = "jpeg" \
        -o $ext = "nef" -o $ext = "png" -o $ext = "tif" -o $ext = "tiff" ]; then

		# retrieve file
		#echo "Retrieving file $DIRNAME"
		if [ ! -f $DIRNAME.$EXT ]; then

			if [ -f $LOCAL_STORE/$PP/$FN ]; then
				# try to copy from LOCAL_STORE
				cp $LOCAL_STORE/$PP/$FN $DIRNAME.$EXT
			else
				# fallback on retrieving from damsrepo
				curl -s -o $DIRNAME.$EXT $DAMS_BASE/files/$TODO

##				# retry failures from openstack
##				if [ ! -f $DIRNAME.$EXT ]; then
##					echo "retrying from openstack"
##					curl -s -o $DIRNAME.$EXT $DAMS_BASE/api/files/$TODO?fs=openStack
##				fi
			fi
		fi

		type=`identify -quiet $DIRNAME.$EXT 2>/dev/null`
		if [ `echo "$type" | grep -c " GIF\|JPEG\|PNG\|TIFF "` -lt 1 ]; then
			if [ $ext != "cr2" -a $ext != "nef" ]; then
				echo "ext: \"$EXT\""
				echo "not JPEG or PNG or TIF, not CR2/NEF raw"
				ERRORS=$(( $ERRORS + 1 ))
				continue
			fi
		fi

		# generate zoomify tiles
		./zoomify-netpbm.sh $DIRNAME.$EXT tmp.zoomify 2>&1
		if [ $? = 0 ]; then
			# remove existing dir
			if [ -d $ZOOM_DIR/$P1/$P2/$DIRNAME ]; then
				rm -r $ZOOM_DIR/$P1/$P2/$DIRNAME #XXX
				if [ $? -ne 0 ]; then
					continue
				fi
			fi

			# move tileset into place
			if [ ! -d $ZOOM_DIR/$P1/$P2 ]; then
				mkdir -p $ZOOM_DIR/$P1/$P2
			fi
			mv tmp.zoomify/$DIRNAME $ZOOM_DIR/$P1/$P2/$DIRNAME

			# remove temp files
			rm *.tif *.pnm *.png *.jpg 2>/dev/null

			# report progress
			PROCESSED=$(( $PROCESSED + 1 ))
			echo `date +"%F %T"` "($PROCESSED) $DIRNAME, $SKIPPED already exist, $ERRORS errors"
		else
			# remove temp files
			rm *.tif *.pnm *.png *.jpg 2>/dev/null

			# report progress
			ERRORS=$(( $ERRORS + 1 ))
			echo "($PROCESSED) $DIRNAME, $SKIPPED already exist, $ERRORS errors"
		fi
	else
		echo "can't process $DIRNAME.$EXT"
	fi
	
##done
echo processed: $PROCESSED
##echo skipped..: $SKIPPED
