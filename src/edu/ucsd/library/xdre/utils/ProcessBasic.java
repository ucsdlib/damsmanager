package edu.ucsd.library.xdre.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;

/**
 * Interface for process execution.
 * @author lsitu@ucsd.edu
**/
public abstract class ProcessBasic
{
    protected String command = null;

    public ProcessBasic()
    {
    }

    /**
     * Create an Process object.
     * @param command Full path to the locally-installed command
    **/
    public ProcessBasic( String command )
    {
        this.command = command;
        if ( !(new File(command)).exists() )
        {
            throw new IllegalArgumentException(
                "Can't find command: " + command
            );
        }
    }

    /**
     * Execute command
     * @param cmd
     * @return
     * @throws Exception
     */
    public boolean execute(ArrayList<String> cmd) throws Exception {

        StringBuffer log = new StringBuffer();
        Reader reader = null;
        InputStream in = null;
        BufferedReader buf = null;
        Process proc = null;
        try
        {
            // execute the process and capture stdout messages
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            proc = pb.start();
            
            in = proc.getInputStream();
            reader = new InputStreamReader(in);
            buf = new BufferedReader(reader);
            for ( String line = null; (line=buf.readLine()) != null; )
            {
                log.append( line + "\n" );
            }
            in.close();
            reader.close();
            buf.close();
            in = null;
            reader = null;
            buf = null;
            // wait for the process to finish
            int status = proc.waitFor();
            if ( status == 0 )
            {
                return true;
            }
            else
            {
                // capture any error messages
                in = proc.getErrorStream();
                reader = new InputStreamReader(in);
                buf = new BufferedReader(reader);
                for ( String line = null; (line=buf.readLine()) != null; )
                {
                    log.append( line + "\n" );
                }
                throw new Exception( log.toString() );
            }
        }
        catch ( Exception ex )
        {
            throw new Exception( log.toString(), ex );
        }finally{
            if(in != null){
                try {
                    in.close();
                    in = null;
                } catch (IOException e) {}
            }
            if(reader != null){
                try {
                    reader.close();
                    reader = null;
                } catch (IOException e) {}
            }
            if(buf != null){
                try {
                    buf.close();
                    buf = null;
                } catch (IOException e) {}
            }
            if(proc != null){
                proc.destroy();
                proc = null;
            }
        }
    }
}
