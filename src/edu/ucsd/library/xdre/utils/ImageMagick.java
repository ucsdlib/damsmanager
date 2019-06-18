package edu.ucsd.library.xdre.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

/**
 * Interface to generate derivatives with ImageMagic.
 * @see http://www.imagemagick.org
 * @author escowles@ucsd.edu
 * @author lsitu@ucsd.edu
**/
public class ImageMagick extends ProcessBasic
{
	private String magick = null;
	public static String WATERMARK_LOCATION = "/darry/watermark";

	public ImageMagick()
	{
		this( "convert" );
	}

	/**
	 * Create an ImageMagick object.
	 * @param magickCommand Full path to the locally-installed ImageMagick
	 *  convert command (typically /usr/bin/convert, /usr/local/bin/convert,
	 *  etc.).
	**/
	public ImageMagick( String magickCommand )
	{
		this.magick = magickCommand;
		if ( !(new File(magickCommand)).exists() )
		{
			throw new IllegalArgumentException(
				"Can't find magick: " + magickCommand
			);
		}
	}

	/**
	 * generate a derivative image for a specific page using image magick
	 * @param src
	 * @param dst
	 * @param width
	 * @param height
	 * @param frameNo
	 * @return
	 * @throws Exception
	 */
	public boolean makeDerivative( File src, File dst, int width, int height, int frameNo ) throws Exception
	{
		return makeDerivative( src, dst, width, height, frameNo, "" );
	}

	/**
	 * generate a derivative image with specific parameters for a specific page using image magick
	 * @param src
	 * @param dst
	 * @param width
	 * @param height
	 * @param frameNo
	 * @param params
	 * @return
	 * @throws Exception
	 */
	public boolean makeDerivative( File src, File dst, int width, int height, int frameNo, String params ) throws Exception
	{
		// build the command
		ArrayList<String> cmd = new ArrayList<String>();
		cmd.add( magick );
		cmd.add( "-auto-orient" ); // auto-rotate images according to metadata
		//cmd.add( "-trim" );        // remove whitespace
		//cmd.add( "+profile" );     // remove EXIF, etc. metadata
		//cmd.add( "'*'" );
		if (StringUtils.isNotBlank(params)) // other parameters
		{
			List<String> paramList = Arrays.asList(params.split(" "));
			for (String param : paramList) 
			{
				if ( param != null && StringUtils.isNotBlank(param = param.trim()) ) 
				{
					cmd.add( param );
				}
			}
		}
		cmd.add( "-resize" );      // resize to specified pixel dimensions
		cmd.add( width + "x" + height );
		cmd.add( src.getAbsolutePath() + (frameNo!=-1?"[" + frameNo + "]":"") );
		cmd.add( dst.getAbsolutePath() );

		return execute(cmd);
	}
}
