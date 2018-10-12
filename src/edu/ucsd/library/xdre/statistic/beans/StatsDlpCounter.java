package edu.ucsd.library.xdre.statistic.beans;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import edu.ucsd.library.xdre.statistic.analyzer.DAMStatistic;

/**
 * Class StatsDlpCounter counting hits like search, browse, collection page, home page for dams
 *
 * @author lsitu@ucsd.edu
 */
public class StatsDlpCounter {
    private static Logger log = Logger.getLogger(StatsDlpCounter.class);

    protected int numSearch = 0;
    protected int numBrowse = 0;
    protected int numColPage = 0;
    protected int numHomePage = 0;

    public int getNumSearch() {
        return numSearch;
    }

    public int getNumBrowse() {
        return numBrowse;
    }

    public int getNumColPage() {
        return numColPage;
    }

    public int getNumHomePage() {
        return numHomePage;
    }

    public boolean addAccess(String[] paths, String paramStr) throws UnsupportedEncodingException{
        boolean pageCounted = false;
        if ( paths.length == 1 ) {
            // home page access.
            numHomePage++;
            pageCounted = true;
        } else if (paths[1].equals("collections") || paths.length > 2 && paths[2].equals("collections")) {
            // Collection home page: /dc/collections, /dc/dlp/collections, /dc/rci/collections
            numColPage++;
            pageCounted = true;
        } else if (paths[1].equals("search") || paths[1].equals("advanced")) {
            if (paths.length == 2) {
                try {
                    // search & browse: /dc/search?q=abc, /dc/advanced/q=abc etc.
                    String id = null;
                    int page = -1;
                    List<String[]> searchParams = new ArrayList<String[]>();
                    List<String[]> browseParams = new ArrayList<String[]>();
                    boolean isColAccess = false;
                    if (StringUtils.isNotBlank(paramStr)) {
                        String[] params = paramStr.split("&");
                        String[] tokens = null;
                        for (int i = 0; i < params.length; i++) {
                            if (StringUtils.isNotBlank(params[i])) {
                                params[i] = URLDecoder.decode(params[i].trim(), "UTF-8");
                                tokens = params[i].split("=");
                                tokens[0] = tokens[0].trim();

                                if (tokens[0].equals("q")) {
                                    searchParams.add(tokens);
                                } else if (tokens[0].startsWith("f[") || tokens[0].startsWith("f%5B")) {

                                    if(tokens[0].indexOf("collection_sim") > 0)
                                        isColAccess = true;
                                } else if (tokens[0].equals("id")) {
                                    id = tokens[1].trim();
                                } else if (tokens[0].equals("page") && tokens.length > 1) {
                                    try {
                                        page = Integer.parseInt(tokens[1].trim());
                                    } catch(NumberFormatException e){}
                                }
                            }
                        }
                    }
    
                    if (isColAccess && id != null) {
                        //Collection access
                        numBrowse++;
                        //increaseCollectionAccess(id, collsAccessMap, collsMap);
                    } else if(browseParams.size() > 0 || page > 0) {
                        //Facet browse
                        numBrowse++;
                    } else {
                        //Search
                        numSearch++;
                    }
                    pageCounted = true;
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid encoding: " + DAMStatistic.toUrl(paths), e);
                }
            } else if (paths.length >= 3 && paths[2].equals("facet")){
                // Facet browse: /dc/search/facet/subject_topic_sim?facet.sort=index
                numBrowse++;
                pageCounted = true;
            }
        }

        return pageCounted;
    }

    public int export(PreparedStatement ps) throws Exception{
        //STATS_DLP insert
        ps.setInt(2, numSearch);
        ps.setInt(3, numBrowse);
        ps.setInt(4, numColPage);
        ps.setInt(5, numHomePage);
        return ps.executeUpdate();
    }

    public String toString(){
        return numSearch + " " + numBrowse + " " + numColPage + " " + numHomePage;
    }
}
