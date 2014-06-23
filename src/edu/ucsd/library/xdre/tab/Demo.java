package edu.ucsd.library.xdre.tab;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;

import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

public class Demo
{
    public static void main( String[] args ) throws Exception
    {
        File f = new File(args[0]);
        File dir = new File(args[1]);
        TabularSource src = new ExcelSource(f);

        OutputFormat pretty = OutputFormat.createPrettyPrint();
        for ( TabularRecord rec = null; (rec = src.nextRecord()) != null; )
        {
            String id = rec.getData().get("object unique id");
            System.out.println("id: " + id);
            
            FileWriter out = new FileWriter( new File(dir, id + ".rdf.xml"));
            XMLWriter writer = new XMLWriter( out, pretty );
            writer.write( rec.toRDFXML() );
            writer.close();
        }

        // stream demo
        TabularInputStream in = new TabularInputStream( new ExcelSource(f) );
        FileOutputStream out = new FileOutputStream( new File(dir, "stream.rdf.xml") );
        for ( int i = -1; (i = in.read()) != -1; )
        {
            out.write( (char)i );
        }
        out.close();
    }
}
