package edu.ucsd.library.xdre.tab;

/**
 * Interface for modules that parse data and return Record instances.
 *
 * @author escowles
 * @since 2014-06-05
**/
public interface RecordSource
{
    /**
     * Get the next TabularRecord available from this source, or null if no
     * more records are available.
    **/
    public Record nextRecord();
}
