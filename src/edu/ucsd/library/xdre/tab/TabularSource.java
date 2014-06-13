package edu.ucsd.library.xdre.tab;

/**
 * Interface for modules that parse tabular data and return TabularRecord
 * instances.
 *
 * @author escowles
 * @since 2014-06-05
**/
public interface TabularSource
{
    /**
     * Get the next TabularRecord available from this source, or null if no
     * more records are available.
    **/
    public TabularRecord nextRecord();
}
