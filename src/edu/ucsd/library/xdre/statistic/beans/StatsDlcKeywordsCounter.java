package edu.ucsd.library.xdre.statistic.beans;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import edu.ucsd.library.xdre.statistic.analyzer.DAMStatistic;

/**
 * Class StatsDlcKeywordsCounter the counter for keywords
 *
 * @author lsitu@ucsd.edu
 */
public class StatsDlcKeywordsCounter {
    private static Logger log = Logger.getLogger(StatsDlcKeywordsCounter.class);

    protected Map<String, Integer> keywordsMap = null;
    protected Map<String, Integer> phrasesMap = null;

    public StatsDlcKeywordsCounter() {
        keywordsMap = new HashMap<String, Integer>();
        phrasesMap = new HashMap<String, Integer>();
    }

    public Map<String, Integer> getKeywordsMap() {
        return keywordsMap;
    }

    public Map<String, Integer> getPhrasesMap() {
        return phrasesMap;
    }

    public void addAccess(String[] paths, String paramStr) throws UnsupportedEncodingException{
        if (paths.length >= 2 && paths.length <= 3) {
            if (paths[1].equalsIgnoreCase("search") && paths.length == 2) {
                try {

                    // search & browse
                    List<String[]> searchParams = new ArrayList<String[]>();
                    List<String[]> browseParams = new ArrayList<String[]>();
                    if (StringUtils.isNotBlank(paramStr)) {
                        String[] params = paramStr.split("&");
                        String[] tokens = null;
                        for (int i = 0; i < params.length; i++) {
                            if (StringUtils.isNotBlank(params[i])) {
                                params[i] = URLDecoder.decode(params[i].trim(), "UTF-8");
                                tokens = params[i].split("=");
                                tokens[0] = tokens[0].trim();
                                if ( tokens[0].equals("q") ) {
                                    searchParams.add(tokens);
                                } else if ( tokens[0].startsWith("f[") || tokens[0].startsWith("f%5B")) {
                                    browseParams.add(tokens);
                                }
                            }
                        }
                    }

                    if (searchParams.size() > 0) {
                        String[] tokens = searchParams.get(0);
                        String keywords = tokens.length == 2 ? tokens[1] : null;
                        if (keywords != null && keywords.length() > 0) {
                            try {
                                parseKeywords(keywords, keywordsMap, phrasesMap);
                            } catch (UnsupportedEncodingException e) {
                                log.info("Unsupported UTF-8 encoding for keywords: '" + keywords + "' in " + DAMStatistic.toUrl(paths));
                                e.printStackTrace();
                            } catch (Exception e) {
                                log.info("Invalid URL encoding for keywords: '" + keywords + "' in " + DAMStatistic.toUrl(paths));
                                e.printStackTrace();
                            }
                        }
                    }
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid encoding: " + DAMStatistic.toUrl(paths), e);
                }
            }
        }
    }

    public int export(PreparedStatement ps, int id) throws Exception{
        //Keywords/Phrases STATS_DLC_KEYWORDS_INSERT insert
        int returnValue = 0;
        Integer counter = null;
        for (String key : keywordsMap.keySet()) {
            counter = keywordsMap.get(key);
            ps.setInt(1, id);
            ps.setString(2, StringEscapeUtils.escapeJava(key).replace("\\\"", "\""));
            ps.setInt(3, counter.intValue());
            ps.setString(4, "keyword");
            returnValue += ps.executeUpdate();
            ps.clearParameters();
        }

        for (String key : phrasesMap.keySet()) {
            counter = phrasesMap.get(key);
            ps.setInt(1, id);
            ps.setString(2, StringEscapeUtils.escapeJava(key).replace("\\\"", "\""));
            ps.setInt(3, counter.intValue());
            ps.setString(4, "phrase");
            returnValue += ps.executeUpdate();
            ps.clearParameters();
        }
        return returnValue;
    }

    public String toString() {
        Iterator<String> it = null;
        String key = null;
        StringBuilder builder = new StringBuilder();
        it = keywordsMap.keySet().iterator();
        while (it.hasNext()) {
            key = (String) it.next();
            builder.append(key + " -> " + keywordsMap.get(key) + "\n");
        }

        it = phrasesMap.keySet().iterator();
        while (it.hasNext()) {
            key = (String) it.next();
            builder.append(key + " -> " + phrasesMap.get(key) + "\n");
        }
        return builder.toString();
    }

    public static void parseKeywords(String keywordsStr, Map<String, Integer> keywordsMap, Map<String, Integer> phrasesMap)
            throws UnsupportedEncodingException{
        String tmp = "";
        int i = 0;
        keywordsStr = URLDecoder.decode(keywordsStr, "UTF-8");
        char[] str = keywordsStr.trim().toCharArray();
        int len = str.length;
        while (i < len) {
            if (str[i]==' ') {
                if (i+3 < len && str[i+1] == 'O' && str[i+2] == 'R' && str[i+3] == ' ') {
                    if (tmp.length() > 0) {
                        addKeyword(tmp, keywordsMap, phrasesMap);
                        tmp = "";
                    }
                    i += 2;
                } else if (i+4 < len && str[i+1] == 'A' && str[i+2] == 'N'  && str[i+3] == 'D' && str[i+4] == ' ') {
                    if(tmp.length() > 0){
                        addKeyword(tmp, keywordsMap, phrasesMap);
                        tmp = "";
                    }
                    i += 3;
                } else if (i+4 < len && str[i+1] == 'N' && str[i+2] == 'O'  && str[i+3] == 'T' && str[i+4] == ' ') {
                    if (tmp.length() > 0) {
                        addKeyword(tmp, keywordsMap, phrasesMap);
                        tmp = "";
                    }
                    i += 3;
                } else if (i+1 < len && (str[i+1] == '\"' || (str[i-1] == '\"' && (i > 2 && str[i-2] != '\\')))) { //Phrase
                    if (tmp.length() > 0) {
                        addKeyword(tmp, keywordsMap, phrasesMap);
                        tmp = "";
                    }
                } else {
                    tmp += str[i];
                }
            } else {
                tmp += str[i];
            }

            i += 1;
        }

        if (tmp.length() > 0) {
            addKeyword(tmp, keywordsMap, phrasesMap);
        }
    }

    public static void addKeyword(String keyword, Map<String, Integer> keywordsMap, Map<String, Integer> phrasesMap) {
        keyword = keyword.trim();
        if (keyword.length()>0) {
            if (keyword.startsWith("\"") && keyword.endsWith("\"")) {
                if(keyword.equals("\"\"") || keyword.equals("\"")) {
                    keyword = "";
                } else {
                    keyword = keyword.substring(1, keyword.length()-1).trim();
                    if(keyword.indexOf(" ") > 0)
                        increaseWordCount(keyword, phrasesMap);
                    else
                        increaseWordCount(keyword, keywordsMap);
                }
            } else {
                increaseWordCount(keyword, keywordsMap);
            }
        }
    }

    public static synchronized void increaseWordCount(String keyword, Map<String, Integer> wordsMap){
            Integer counter = wordsMap.get(keyword);
            if (counter == null) {
                counter = new Integer(1);
            } else {
                counter = new Integer(counter.intValue()+1);
            }
            wordsMap.put(keyword, counter);
    }
}
