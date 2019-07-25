<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
      xmlns:dams="http://library.ucsd.edu/ontology/dams#"
      xmlns:mads="http://www.loc.gov/mads/rdf/v1#"
      xmlns:owl="http://www.w3.org/2002/07/owl#"
      xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
      xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
      xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
      exclude-result-prefixes="dams mads owl rdf rdfs">
  <xsl:output method="text" encoding="utf-8" version="4.0"/>

    <xsl:template match="rdf:RDF">
        <xsl:call-template name="startJsonObject"/>

        <xsl:for-each select="*[contains(local-name(), 'Object') or contains(local-name(), 'Collection')]">
            <xsl:variable name="id"><xsl:value-of select="@rdf:about"/></xsl:variable>
            <xsl:variable name="count"><xsl:number level="any" count="dams:Object"/></xsl:variable>
            <xsl:variable name="objectId">
                <xsl:choose>
                  <xsl:when test="@rdf:about = 'ARK'"><xsl:value-of select="concat($id, '#', $count)"/></xsl:when>
                  <xsl:otherwise><xsl:value-of select="substring-after(@rdf:about, '/20775/')" /></xsl:otherwise>
                </xsl:choose>
            </xsl:variable>
            <xsl:call-template name="toQuotedValue">
                <xsl:with-param name="val"><xsl:value-of select="concat($id, '#', $count)"/></xsl:with-param>
            </xsl:call-template>
            <xsl:text>:</xsl:text>
            <xsl:call-template name="startJsonArray"/>
    
            <xsl:call-template name="appendJsonObject">
                <xsl:with-param name="key">Object Unique ID</xsl:with-param>
                <xsl:with-param name="val"><xsl:value-of select="$objectId"/></xsl:with-param>
            </xsl:call-template>

            <xsl:call-template name="appendJsonObject">
                <xsl:with-param name="key">Level</xsl:with-param>
                <xsl:with-param name="val"><xsl:call-template name="damsResource"/></xsl:with-param>
            </xsl:call-template>

            <xsl:call-template name="damsElements"/>

            <xsl:for-each select="dams:hasComponent/dams:Component">
                <xsl:sort select="dams:order" data-type="number" order="ascending" />
                <xsl:call-template name="damsComponent">
                    <xsl:with-param name="objectId"><xsl:value-of select="substring-after(@rdf:about, '/20775/')" /></xsl:with-param>
                   <xsl:with-param name="depth">1</xsl:with-param>
               </xsl:call-template>
            </xsl:for-each>

            <xsl:call-template name="endJsonArray"/>
            <xsl:text>,</xsl:text>
        </xsl:for-each>

        <xsl:call-template name="endJsonObject"/>
    </xsl:template>

    <xsl:template name="damsElements">
        <!-- dams:File -->
        <xsl:for-each select="*[local-name() = 'hasFile']">
            <xsl:if test="contains(dams:File/dams:use, '-source') or contains(dams:File/@rdf:about, '/1.')">
                <xsl:apply-templates />
            </xsl:if>
        </xsl:for-each>

        <!-- dams:typeOfResource -->
        <xsl:for-each select="*[local-name()='typeOfResource']">
            <xsl:sort select="."/>
            <xsl:call-template name="appendJsonObject">
                <xsl:with-param name="key">Type of Resource</xsl:with-param>
                <xsl:with-param name="val"><xsl:value-of select="."/></xsl:with-param>
            </xsl:call-template>
        </xsl:for-each>

        <!-- mads:Language -->
        <xsl:for-each select="*[local-name()='language']">
           <xsl:variable name='resArk'><xsl:value-of select="*/@rdf:about | @rdf:resource"/></xsl:variable>
           <xsl:choose>
               <xsl:when test="$resArk != ''">
                    <xsl:apply-templates select="//*[@rdf:about=$resArk]" />
                </xsl:when>
                <xsl:otherwise>
                    <xsl:apply-templates />
                </xsl:otherwise>
           </xsl:choose>
        </xsl:for-each>

        <!-- mads:Title -->
        <xsl:for-each select="*[local-name()='title']">
            <xsl:apply-templates />
        </xsl:for-each>

        <!-- dams:Relationship -->
        <xsl:for-each select="*[local-name()='relationship']">
            <xsl:apply-templates />
        </xsl:for-each>

        <!-- dams:Date -->
        <xsl:for-each select="*[local-name()='date']">
            <xsl:apply-templates />
        </xsl:for-each>

        <!-- dams:Copyright -->
        <xsl:for-each select="*[local-name()='copyright']">
            <xsl:variable name='resArk'><xsl:value-of select="*/@rdf:about | @rdf:resource"/></xsl:variable>
            <xsl:choose>
                <xsl:when test="$resArk != ''">
                    <xsl:apply-templates select="//*[@rdf:about=$resArk]" />
                </xsl:when>
                <xsl:otherwise>
                    <xsl:apply-templates />
                </xsl:otherwise>
              </xsl:choose>
        </xsl:for-each>

        <!-- RightsHolder(s) -->
        <xsl:for-each select="*[contains(local-name(), 'rightsHolder')]">
            <xsl:variable name='resArk'><xsl:value-of select="*/@rdf:about | @rdf:resource"/></xsl:variable>
            <xsl:choose>
                <xsl:when test="$resArk != ''">
                    <xsl:variable name="ark"><xsl:value-of select="substring-after($resArk, '/20775/')" /></xsl:variable>
                    <xsl:call-template name="appendJsonObject">
                        <xsl:with-param name="key"><xsl:value-of select="local-name()"/></xsl:with-param>
                        <xsl:with-param name="val"><xsl:value-of select="concat(//*[@rdf:about=$resArk]/mads:authoritativeLabel, ' @ ', $ark)"/></xsl:with-param>
                    </xsl:call-template>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:call-template name="appendJsonObject">
                        <xsl:with-param name="key"><xsl:value-of select="local-name()"/></xsl:with-param>
                        <xsl:with-param name="val"><xsl:value-of select="*/mads:authoritativeLabel"/></xsl:with-param>
                    </xsl:call-template>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:for-each>

        <!-- dams:OtherRights -->
        <xsl:for-each select="*[local-name()='otherRights']">
            <xsl:variable name='resArk'><xsl:value-of select="*/@rdf:about | @rdf:resource"/></xsl:variable>
            <xsl:choose>
                <xsl:when test="$resArk != ''">
                    <xsl:apply-templates select="//*[@rdf:about=$resArk]" />
                </xsl:when>
                <xsl:otherwise>
                    <xsl:apply-templates />
                </xsl:otherwise>
              </xsl:choose>
        </xsl:for-each>

        <!-- mads:License -->
        <xsl:for-each select="*[local-name()='license']">
            <xsl:variable name='resArk'><xsl:value-of select="*/@rdf:about | @rdf:resource"/></xsl:variable>
            <xsl:choose>
                <xsl:when test="$resArk != ''">
                    <xsl:apply-templates select="//*[@rdf:about=$resArk]" />
                </xsl:when>
                <xsl:otherwise>
                    <xsl:apply-templates />
                </xsl:otherwise>
              </xsl:choose>
        </xsl:for-each>

        <!-- Collection(s) -->
        <xsl:if test="*[contains(local-name(), 'Collection')]">
            <xsl:variable name="collections">
                <xsl:for-each select="*[contains(local-name(), 'Collection')]">
                    <xsl:sort select="*/@rdf:about | @rdf:resource"/>
                    <xsl:variable name='resArk'><xsl:value-of select="*/@rdf:about | @rdf:resource"/></xsl:variable>
                    <xsl:variable name="ark"><xsl:value-of select="substring-after($resArk, '/20775/')" /></xsl:variable>
                    <xsl:value-of select="concat('|', //*[@rdf:about=$resArk]/dams:title//mads:authoritativeLabel, ' @ ', $ark)"/>
                </xsl:for-each>
            </xsl:variable>
            <xsl:call-template name="appendJsonObject">
               <xsl:with-param name="key">collection(s)</xsl:with-param>
               <xsl:with-param name="val"><xsl:value-of select="substring-after($collections, '|')"/></xsl:with-param>
            </xsl:call-template>
        </xsl:if>

        <!-- dams:ScopeContentNote -->
        <xsl:for-each select="*[local-name()='scopeContentNote']/*">
            <xsl:call-template name="damsScopeContentNote"/>
        </xsl:for-each>

        <!-- dams:Note[type='identifier'] -->
        <xsl:for-each select="*[local-name()='note' and dams:Note/dams:type='identifier']/*">
            <xsl:call-template name="damsNoteIdentifier"/>
        </xsl:for-each>

        <!-- dams:Note[dams:type='statement of responsibility'] -->
        <xsl:for-each select="*[local-name()='note' and dams:Note/dams:type='statement of responsibility']/*">
            <xsl:call-template name="damsNoteStatementOfResponsibility"/>
        </xsl:for-each>

        <!-- dasm:Note[dams:type='local added entry'] -->
        <xsl:for-each select="*[local-name()='note' and dams:Note/dams:type='local added entry']/*">
            <xsl:call-template name="damsNoteLocalAddedEntry"/>
        </xsl:for-each>

        <!-- Other Notes -->
        <xsl:for-each select="*[local-name()='note' and not(contains(dams:Note/dams:type,'identifier')) and not(contains(dams:Note/dams:type,'local added entry')) and not(contains(dams:Note/dams:type,'statement of responsibility'))]">
            <xsl:sort select="*/dams:type"/>
            <xsl:variable name='resArk'><xsl:value-of select="*/@rdf:about | @rdf:resource"/></xsl:variable>
            <xsl:choose>
                <xsl:when test="$resArk != ''">
                    <xsl:apply-templates select="//*[@rdf:about=$resArk]" />
                </xsl:when>
                <xsl:otherwise>
                    <xsl:apply-templates />
                </xsl:otherwise>
              </xsl:choose>
        </xsl:for-each>

        <!-- dams:Subject -->
        <xsl:for-each select="*[local-name() = 'anatomy' or local-name() = 'commonName' or local-name() = 'cruise' or local-name() = 'culturalContext' or local-name() = 'lithology' or local-name() = 'scientificName' or local-name() = 'series']">
            <xsl:sort select="name()" order="descending"/>
            <xsl:variable name='resArk'><xsl:value-of select="*/@rdf:about | @rdf:resource"/></xsl:variable>
            <xsl:variable name="columnName">
                <xsl:choose>
                    <xsl:when test="local-name() = 'commonName'">common name</xsl:when>
                    <xsl:when test="local-name() = 'scientificName'">scientific name</xsl:when>
                    <xsl:when test="local-name() = 'culturalContext'">culturalContext</xsl:when>
                    <xsl:otherwise><xsl:value-of select="translate(local-name(),'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')"/></xsl:otherwise>
                </xsl:choose>
            </xsl:variable>
            <xsl:choose>
                <xsl:when test="$resArk !=''">
                    <xsl:call-template name="subject">
                        <xsl:with-param name="columnName">Subject:<xsl:value-of select="$columnName"/></xsl:with-param>
                        <xsl:with-param name="node" select="//*[@rdf:about=$resArk]" />
                    </xsl:call-template>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:call-template name="subject">
                        <xsl:with-param name="columnName">Subject:<xsl:value-of select="$columnName "/></xsl:with-param>
                        <xsl:with-param name="node" select="*" />
                    </xsl:call-template>
                </xsl:otherwise>
              </xsl:choose>
        </xsl:for-each>

        <!-- mads:Subject -->
        <xsl:for-each select="*[local-name() = 'conferenceName' or local-name() = 'corporateName' or local-name() = 'familyName' or local-name() = 'genreForm' or local-name() = 'geographic' or local-name() = 'occupation' or local-name() = 'personalName' or local-name() = 'temporal' or local-name() = 'topic']">
            <xsl:sort select="name()" order="descending"/>
            <xsl:variable name='resArk'><xsl:value-of select="*/@rdf:about | @rdf:resource"/></xsl:variable>
            <xsl:variable name="columnName">
                <xsl:choose>
                    <xsl:when test="local-name() = 'conferenceName'">conference name</xsl:when>
                    <xsl:when test="local-name() = 'corporateName'">corporate name</xsl:when>
                    <xsl:when test="local-name() = 'familyName'">family name</xsl:when>
                    <xsl:when test="local-name() = 'personalName'">personal name</xsl:when>
                    <xsl:when test="local-name() = 'genreForm'">genre</xsl:when>
                    <xsl:otherwise><xsl:value-of select="translate(local-name(),'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')"/></xsl:otherwise>
                </xsl:choose>
            </xsl:variable>
            <xsl:choose>
                <xsl:when test="$resArk !=''">
                    <xsl:call-template name="subject">
                        <xsl:with-param name="columnName">Subject:<xsl:value-of select="$columnName"/></xsl:with-param>
                        <xsl:with-param name="node" select="//*[@rdf:about=$resArk]" />
                    </xsl:call-template>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:call-template name="subject">
                        <xsl:with-param name="columnName">Subject:<xsl:value-of select="$columnName "/></xsl:with-param>
                        <xsl:with-param name="node" select="*" />
                    </xsl:call-template>
                </xsl:otherwise>
              </xsl:choose>
        </xsl:for-each>

        <!-- dams:ComplexSubject -->
        <xsl:for-each select="*[local-name() = 'complexSubject']">
            <xsl:variable name='resArk'><xsl:value-of select="*/@rdf:about | @rdf:resource"/></xsl:variable>
            <xsl:choose>
                <xsl:when test="$resArk != ''">
                    <xsl:apply-templates select="//*[@rdf:about=$resArk]" />
                </xsl:when>
                <xsl:otherwise>
                    <xsl:apply-templates select="*" />
                </xsl:otherwise>
              </xsl:choose>
        </xsl:for-each>

        <!-- dams:RelatedResource -->
        <xsl:for-each select="*[local-name()='relatedResource']">
            <xsl:variable name='resArk'><xsl:value-of select="*/@rdf:about | @rdf:resource"/></xsl:variable>
            <xsl:choose>
                <xsl:when test="$resArk != ''">
                    <xsl:apply-templates select="//*[@rdf:about=$resArk]" />
                </xsl:when>
                <xsl:otherwise>
                    <xsl:apply-templates />
                </xsl:otherwise>
            </xsl:choose>
        </xsl:for-each>

        <!-- dams:Cartographics -->
        <xsl:for-each select="*[local-name()='cartographics']">
           <xsl:variable name='resArk'><xsl:value-of select="*/@rdf:about | @rdf:resource"/></xsl:variable>
           <xsl:choose>
             <xsl:when test="$resArk != ''">
                 <xsl:apply-templates select="//*[@rdf:about=$resArk]" />
             </xsl:when>
             <xsl:otherwise>
                 <xsl:apply-templates />
             </xsl:otherwise>
           </xsl:choose>
        </xsl:for-each>
    </xsl:template>

    <xsl:template name="madsTitle" match="mads:Title">
       <xsl:for-each select="mads:elementList">
           <xsl:for-each select="*[local-name() != 'NonSortElement']">
               <xsl:variable name="titleName">
                   <xsl:choose>
                       <xsl:when test="local-name()='MainTitleElement'">Title</xsl:when>
                       <xsl:when test="local-name()='SubTitleElement'">Subtitle</xsl:when>
                       <xsl:when test="local-name()='PartNameElement'">Part name</xsl:when>
                       <xsl:when test="local-name()='PartNumberElement'">Part number</xsl:when>
                       <xsl:otherwise><xsl:value-of select="substring-before(local-name(), 'Element')"/></xsl:otherwise>
                   </xsl:choose>
               </xsl:variable>
                <xsl:call-template name="appendJsonObject">
                    <xsl:with-param name="key"><xsl:value-of select="$titleName"/></xsl:with-param>
                    <xsl:with-param name="val">
                        <xsl:choose>
                            <xsl:when test="../mads:NonSortElement"><xsl:value-of select="concat(../mads:NonSortElement,' ',mads:elementValue)"/></xsl:when>
                            <xsl:otherwise><xsl:value-of select="mads:elementValue"/></xsl:otherwise>
                        </xsl:choose>
                    </xsl:with-param>
                </xsl:call-template>
            </xsl:for-each>
        </xsl:for-each>
        <xsl:for-each select="*[contains(local-name(), 'Variant')]">
            <xsl:variable name="variantName">
              <xsl:choose>
                   <xsl:when test="local-name()='hasTranslationVariant'">Translation</xsl:when>
                   <xsl:otherwise>Variant</xsl:otherwise>
               </xsl:choose>
            </xsl:variable>
            <xsl:call-template name="appendJsonObject">
                <xsl:with-param name="key"><xsl:value-of select="$variantName" /></xsl:with-param>
                <xsl:with-param name="val"><xsl:value-of select="mads:Variant/mads:variantLabel"/></xsl:with-param>
            </xsl:call-template>
        </xsl:for-each>
    </xsl:template>

    <xsl:template name="subject">
        <xsl:param name="columnName"/>
        <xsl:param name="node"/>
        <xsl:variable name="ark"><xsl:value-of select="substring-after($node/@rdf:about, '/20775/')" /></xsl:variable>
        <xsl:variable name="hasExactExternalAuthority" select="$node/mads:hasExactExternalAuthority/@rdf:resource" />
        <xsl:variable name="fastHeadings">
          <xsl:choose>
            <xsl:when test="contains($hasExactExternalAuthority, 'id.worldcat.org/fast/')"> FAST</xsl:when>
            <xsl:otherwise></xsl:otherwise>
          </xsl:choose>
        </xsl:variable>
        <xsl:call-template name="appendJsonObject">
           <xsl:with-param name="key"><xsl:value-of select="$columnName"/><xsl:value-of select="$fastHeadings"/></xsl:with-param>
           <xsl:with-param name="val"><xsl:value-of select="concat($node/mads:authoritativeLabel, ' @ ', $ark)"/></xsl:with-param>
        </xsl:call-template>
    </xsl:template>

    <xsl:template name="complexSubject" match="mads:ComplexSubject">
        <xsl:variable name="ark"><xsl:value-of select="substring-after(@rdf:about, '/20775/')" /></xsl:variable>
        <xsl:variable name="subjectName" select="local-name(mads:componentList/*[1])"/>
        <xsl:variable name="hasExactExternalAuthority" select="mads:componentList/*[1]/mads:hasExactExternalAuthority/@rdf:resource" />
        <xsl:variable name="fastHeadings">
          <xsl:choose>
            <xsl:when test="starts-with($hasExactExternalAuthority, 'http://id.worldcat.org/fast/')"> FAST</xsl:when>
            <xsl:otherwise></xsl:otherwise>
          </xsl:choose>
        </xsl:variable>
        <xsl:variable name="columnName">
            <xsl:choose>
                <xsl:when test="$subjectName = 'ConferenceName'">conference name</xsl:when>
                <xsl:when test="$subjectName = 'CorporateName'">corporate name</xsl:when>
                <xsl:when test="$subjectName = 'FamilyName'">family name'</xsl:when>
                <xsl:when test="$subjectName = 'PersonalName'">personal name</xsl:when>
                <xsl:when test="$subjectName = 'GenreForm'">genre</xsl:when>
                <xsl:otherwise><xsl:value-of select="translate($subjectName,'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')"/></xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:call-template name="appendJsonObject">
           <xsl:with-param name="key">Subject:<xsl:value-of select="$columnName"/></xsl:with-param>
           <xsl:with-param name="val"><xsl:value-of select="concat(mads:authoritativeLabel, ' @ ', $ark)"/></xsl:with-param>
        </xsl:call-template>
    </xsl:template>

    <xsl:template name="madsLanguage" match="mads:Language">
        <xsl:variable name="ark"><xsl:value-of select="substring-after(@rdf:about, '/20775/')" /></xsl:variable>
        <xsl:call-template name="appendJsonObject">
           <xsl:with-param name="key">Language</xsl:with-param>
           <xsl:with-param name="val"><xsl:value-of select="mads:code"/> - <xsl:value-of select="concat(mads:authoritativeLabel, ' @ ', $ark)"/></xsl:with-param>
        </xsl:call-template>
    </xsl:template>

    <xsl:template name="damsRelationship" match="dams:Relationship">
        <xsl:variable name="roleArk" select="dams:role//@rdf:about | dams:role/@rdf:resource" />
        <xsl:variable name="nameArk" select="*[contains(local-name(), 'Name')]//@rdf:about | *[contains(local-name(), 'Name')]/@rdf:resource" />
        <xsl:variable name="ark"><xsl:value-of select="substring-after($nameArk, '/20775/')" /></xsl:variable>
        <xsl:variable name="role">
          <xsl:value-of select="//mads:Authority[@rdf:about=$roleArk]/mads:authoritativeLabel | //mads:Authority[@rdf:about=$roleArk]/rdf:value"/>
        </xsl:variable>
        <xsl:variable name="name">
            <xsl:choose>
                <xsl:when test="*[contains(local-name(), 'personal')]">Person</xsl:when>
                <xsl:when test="*[contains(local-name(), 'corporate')]">Corporate</xsl:when>
                <xsl:otherwise>Name</xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:call-template name="appendJsonObject">
           <xsl:with-param name="key"><xsl:value-of select="$name"/>:<xsl:value-of select="$role"/></xsl:with-param>
           <xsl:with-param name="val">
             <xsl:value-of select="concat(//*[@rdf:about=$nameArk]/mads:authoritativeLabel, ' @ ', $ark)"/>
           </xsl:with-param>
        </xsl:call-template>
    </xsl:template>

    <xsl:template name="damsDate" match="dams:Date">
        <xsl:variable name="type">
            <xsl:choose>
                <xsl:when test="string-length(dams:type) > 0"><xsl:value-of select="dams:type"/></xsl:when>
                <xsl:otherwise>creation</xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:call-template name="appendJsonObject">
           <xsl:with-param name="key">Date:<xsl:value-of select="$type"/></xsl:with-param>
           <xsl:with-param name="val"><xsl:value-of select="rdf:value"/></xsl:with-param>
        </xsl:call-template>
        <xsl:if test="dams:beginDate">
            <xsl:call-template name="appendJsonObject">
               <xsl:with-param name="key">Begin date</xsl:with-param>
               <xsl:with-param name="val"><xsl:value-of select="dams:beginDate"/></xsl:with-param>
            </xsl:call-template>
        </xsl:if>
        <xsl:if test="dams:endDate">
            <xsl:call-template name="appendJsonObject">
               <xsl:with-param name="key">End date</xsl:with-param>
               <xsl:with-param name="val"><xsl:value-of select="dams:endDate"/></xsl:with-param>
            </xsl:call-template>
        </xsl:if>
    </xsl:template>

    <xsl:template name="damsCopyright" match="dams:Copyright">
        <xsl:for-each select="*">
            <xsl:choose>
                <xsl:when test="local-name() = 'copyrightJurisdiction'">
                    <xsl:call-template name="appendJsonObject">
                       <xsl:with-param name="key">copyrightJurisdiction</xsl:with-param>
                       <xsl:with-param name="val"><xsl:value-of select="."/></xsl:with-param>
                    </xsl:call-template>
                </xsl:when>
                <xsl:when test="local-name() = 'copyrightStatus'">
                    <xsl:call-template name="appendJsonObject">
                       <xsl:with-param name="key">copyrightStatus</xsl:with-param>
                       <xsl:with-param name="val"><xsl:value-of select="."/></xsl:with-param>
                    </xsl:call-template>
                </xsl:when>
                <xsl:when test="local-name() = 'copyrightPurposeNote'">
                    <xsl:call-template name="appendJsonObject">
                       <xsl:with-param name="key">copyrightPurposeNote</xsl:with-param>
                       <xsl:with-param name="val"><xsl:value-of select="."/></xsl:with-param>
                    </xsl:call-template>
                </xsl:when>
                <xsl:when test="local-name() = 'copyrightNote'">
                    <xsl:call-template name="appendJsonObject">
                       <xsl:with-param name="key">copyrightNote</xsl:with-param>
                       <xsl:with-param name="val"><xsl:value-of select="."/></xsl:with-param>
                    </xsl:call-template>
                </xsl:when>
            </xsl:choose>
        </xsl:for-each>
    </xsl:template>

    <xsl:template name="damsOtherRights" match="dams:OtherRights">
        <xsl:for-each select="*">
            <xsl:choose>
                <xsl:when test="local-name() = 'otherRightsBasis'">
                    <xsl:call-template name="appendJsonObject">
                       <xsl:with-param name="key">otherRights:otherRightsBasis</xsl:with-param>
                       <xsl:with-param name="val"><xsl:value-of select="."/></xsl:with-param>
                    </xsl:call-template>
                </xsl:when>
                <xsl:when test="local-name() = 'otherRightsNote'">
                    <xsl:call-template name="appendJsonObject">
                       <xsl:with-param name="key">otherRights:otherRightsNote</xsl:with-param>
                       <xsl:with-param name="val"><xsl:value-of select="."/></xsl:with-param>
                    </xsl:call-template>
                </xsl:when>
            </xsl:choose>
        </xsl:for-each>
        <xsl:for-each select="dams:permission[1]/dams:Permission/dams:type">
            <xsl:call-template name="appendJsonObject">
               <xsl:with-param name="key">otherRights:permission/type</xsl:with-param>
               <xsl:with-param name="val"><xsl:value-of select="."/></xsl:with-param>
            </xsl:call-template>
        </xsl:for-each>
        <xsl:for-each select="dams:restriction[1]/dams:Restriction/dams:type">
            <xsl:call-template name="appendJsonObject">
               <xsl:with-param name="key">otherRights:restriction/type</xsl:with-param>
               <xsl:with-param name="val"><xsl:value-of select="."/></xsl:with-param>
            </xsl:call-template>
        </xsl:for-each>
    </xsl:template>

    <xsl:template name="damsLicense" match="dams:License">
        <xsl:for-each select="*">
            <xsl:choose>
                <xsl:when test="local-name() = 'beginDate'">
                    <xsl:call-template name="appendJsonObject">
                       <xsl:with-param name="key">license:beginDate</xsl:with-param>
                       <xsl:with-param name="val"><xsl:value-of select="."/></xsl:with-param>
                    </xsl:call-template>
                </xsl:when>
                <xsl:when test="local-name() = 'endDate'">
                    <xsl:call-template name="appendJsonObject">
                       <xsl:with-param name="key">license:endDate</xsl:with-param>
                       <xsl:with-param name="val"><xsl:value-of select="."/></xsl:with-param>
                    </xsl:call-template>
                </xsl:when>
                <xsl:when test="local-name() = 'licenseNote'">
                    <xsl:call-template name="appendJsonObject">
                       <xsl:with-param name="key">license:licenseNote</xsl:with-param>
                       <xsl:with-param name="val"><xsl:value-of select="."/></xsl:with-param>
                    </xsl:call-template>
                </xsl:when>
                <xsl:when test="local-name() = 'licenseURI'">
                    <xsl:call-template name="appendJsonObject">
                       <xsl:with-param name="key">license:licenseURI</xsl:with-param>
                       <xsl:with-param name="val"><xsl:value-of select="."/></xsl:with-param>
                    </xsl:call-template>
                </xsl:when>
            </xsl:choose>
        </xsl:for-each>
        <xsl:for-each select="dams:permission[1]/dams:Permission/dams:type">
            <xsl:call-template name="appendJsonObject">
               <xsl:with-param name="key">license:permission/type</xsl:with-param>
               <xsl:with-param name="val"><xsl:value-of select="."/></xsl:with-param>
            </xsl:call-template>
        </xsl:for-each>
        <xsl:for-each select="dams:restriction[1]/dams:Restriction/dams:type">
            <xsl:call-template name="appendJsonObject">
               <xsl:with-param name="key">license:restriction/type</xsl:with-param>
               <xsl:with-param name="val"><xsl:value-of select="."/></xsl:with-param>
            </xsl:call-template>
        </xsl:for-each>
    </xsl:template>

    <xsl:template name="damsScopeContentNote" match="dams:ScopeContentNote">
        <xsl:call-template name="appendJsonObject">
           <xsl:with-param name="key">Brief description</xsl:with-param>
           <xsl:with-param name="val"><xsl:value-of select="rdf:value"/></xsl:with-param>
        </xsl:call-template>
    </xsl:template>

    <xsl:template name="damsNoteIdentifier" match="dams:Note[dams:type='identifier']" priority="1">
        <xsl:variable name="label">
           <xsl:choose>
               <xsl:when test="dams:displayLabel='Roger record'">roger record</xsl:when>
               <xsl:otherwise><xsl:value-of select="dams:displayLabel"/></xsl:otherwise>
           </xsl:choose>
        </xsl:variable>
        <xsl:call-template name="appendJsonObject">
           <xsl:with-param name="key">Identifier:<xsl:value-of select="$label"/></xsl:with-param>
           <xsl:with-param name="val"><xsl:value-of select="rdf:value"/></xsl:with-param>
        </xsl:call-template>
    </xsl:template>

    <xsl:template name="damsNoteLocalAddedEntry" match="dams:Note[dams:type='local added entry']" priority="1">
        <xsl:call-template name="appendJsonObject">
           <xsl:with-param name="key">79X Local Added Entry</xsl:with-param>
           <xsl:with-param name="val"><xsl:value-of select="rdf:value"/></xsl:with-param>
        </xsl:call-template>
    </xsl:template>

    <xsl:template name="damsNoteStatementOfResponsibility" match="dams:Note[dams:type='statement of responsibility']" priority="1">
        <xsl:call-template name="appendJsonObject">
           <xsl:with-param name="key">Note:statement of responsibility</xsl:with-param>
           <xsl:with-param name="val"><xsl:value-of select="rdf:value"/></xsl:with-param>
        </xsl:call-template>
    </xsl:template>

    <xsl:template name="damsNote" match="dams:Note">
        <xsl:call-template name="appendJsonObject">
           <xsl:with-param name="key">Note:<xsl:value-of select="dams:type"/></xsl:with-param>
           <xsl:with-param name="val"><xsl:value-of select="rdf:value"/></xsl:with-param>
        </xsl:call-template>
    </xsl:template>

    <xsl:template name="damsRelatedResource" match="dams:RelatedResource">
        <xsl:variable name="uri">
            <xsl:choose>
                <xsl:when test="string-length(dams:uri/@rdf:resource) > 0"><xsl:value-of select="dams:uri/@rdf:resource"/></xsl:when>
                <xsl:otherwise><xsl:value-of select="dams:uri"/></xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:call-template name="appendJsonObject">
            <xsl:with-param name="key">Related resource:<xsl:value-of select="dams:type"/></xsl:with-param>
            <xsl:with-param name="val"><xsl:value-of select="dams:description"/> @ <xsl:value-of select="$uri"/></xsl:with-param>
        </xsl:call-template>
    </xsl:template>

    <xsl:template name="damsCartographics" match="dams:Cartographics">
        <xsl:for-each select="*">
            <xsl:variable name="columnName">
                <xsl:choose>
                    <xsl:when test="local-name() = 'referenceSystem'">reference system</xsl:when>
                    <xsl:otherwise><xsl:value-of select="local-name()"/></xsl:otherwise>
                </xsl:choose>
            </xsl:variable>
            <xsl:call-template name="appendJsonObject">
               <xsl:with-param name="key">Geographic:<xsl:value-of select="$columnName"/></xsl:with-param>
               <xsl:with-param name="val"><xsl:value-of select="."/></xsl:with-param>
            </xsl:call-template>
        </xsl:for-each>
    </xsl:template>

    <xsl:template name="damsResource">
        <xsl:value-of select="local-name()"/>
    </xsl:template>

    <xsl:template name="damsFile" match="dams:File">
        <xsl:variable name="fileID"><xsl:value-of select="@rdf:about"/></xsl:variable>
        <xsl:variable name="use"><xsl:value-of select="dams:use"/></xsl:variable>
        <xsl:if test="contains($fileID, '/1.') or contains($use, 'source')">
            <xsl:variable name="fileName">
                <xsl:choose>
                   <xsl:when test="contains($fileID, '/1.')">File name</xsl:when>
                   <xsl:otherwise>File name 2</xsl:otherwise>
               </xsl:choose>
            </xsl:variable>
            <xsl:variable name="use">
                <xsl:choose>
                   <xsl:when test="contains($fileID, '/1.')">File use</xsl:when>
                   <xsl:otherwise>File use 2</xsl:otherwise>
                </xsl:choose>
            </xsl:variable>

            <xsl:call-template name="appendJsonObject">
               <xsl:with-param name="key"><xsl:value-of select="$fileName"/></xsl:with-param>
               <xsl:with-param name="val"><xsl:value-of select="dams:sourceFileName"/></xsl:with-param>
            </xsl:call-template>
            <xsl:call-template name="appendJsonObject">
               <xsl:with-param name="key"><xsl:value-of select="$use"/></xsl:with-param>
               <xsl:with-param name="val"><xsl:value-of select="dams:use"/></xsl:with-param>
            </xsl:call-template>
        </xsl:if>
    </xsl:template>

    <xsl:template name="damsComponent">
      <xsl:param name="objectId"/>
      <xsl:param name="depth"/>
      <xsl:call-template name="startJsonObject"/>
       <xsl:call-template name="toQuotedValue">
            <xsl:with-param name="val">Component|<xsl:value-of select="@rdf:about"/></xsl:with-param>
        </xsl:call-template>
        <xsl:text>:</xsl:text>
        <xsl:call-template name="startJsonArray"/>
            <xsl:call-template name="appendJsonObject">
                <xsl:with-param name="key">Object Unique ID</xsl:with-param>
                <xsl:with-param name="val"><xsl:value-of select="$objectId"/></xsl:with-param>
            </xsl:call-template>

            <xsl:call-template name="appendJsonObject">
                <xsl:with-param name="key">Level</xsl:with-param>
                <xsl:with-param name="val">
                    <xsl:choose>
                        <xsl:when test="$depth = '1'">Component</xsl:when>
                        <xsl:otherwise>Sub-component</xsl:otherwise>
                    </xsl:choose>
                </xsl:with-param>
            </xsl:call-template>

            <xsl:call-template name="damsElements"/>

            <xsl:for-each select="dams:hasComponent/dams:Component">
                <xsl:sort select="dams:order" data-type="number" order="ascending" />
                <xsl:call-template name="damsComponent">
                   <xsl:with-param name="objectId"><xsl:value-of select="substring-after(@rdf:about, '/20775/')" /></xsl:with-param>
                   <xsl:with-param name="depth"><xsl:value-of select="$depth + 1"/></xsl:with-param>
               </xsl:call-template>
            </xsl:for-each>

        <xsl:call-template name="endJsonArray"/>
        <xsl:call-template name="endJsonObject"/>
        <xsl:text>,</xsl:text>
    </xsl:template>

    <xsl:template name="string-replace">
        <xsl:param name="text" />
        <xsl:param name="replace" />
        <xsl:param name="by" />
        <xsl:choose>
            <xsl:when test="$text = '' or $replace = '' or not($replace)" >
                <xsl:value-of select="$text" />
            </xsl:when>
            <xsl:when test="contains($text, $replace)">
                <xsl:value-of select="substring-before($text,$replace)" />
                <xsl:value-of select="$by" />
                <xsl:call-template name="string-replace">
                    <xsl:with-param name="text" select="substring-after($text,$replace)" />
                    <xsl:with-param name="replace" select="$replace" />
                    <xsl:with-param name="by" select="$by" />
                </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$text" />
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="appendJsonObject">
        <xsl:param name="key"/>
        <xsl:param name="val"/>

        <xsl:call-template name="startJsonObject"/>
            <xsl:call-template name="toQuotedValue">
                <xsl:with-param name="val"><xsl:value-of select="$key"/></xsl:with-param>
            </xsl:call-template>
            <xsl:text>:</xsl:text>
            <xsl:call-template name="toQuotedValue">
                 <xsl:with-param name="val"><xsl:value-of select="$val"/></xsl:with-param>
            </xsl:call-template>
        <xsl:call-template name="endJsonObject"/>
        <xsl:text>,</xsl:text>
    </xsl:template>

    <xsl:template name="toQuotedValue">
        <xsl:param name="val"/>
        <xsl:variable name="escapedVal">
            <xsl:call-template name="string-replace">
                <xsl:with-param name="text" select="$val" />
                <xsl:with-param name="replace" select="'&quot;'" />
                <xsl:with-param name="by" select="'\&quot;'" />
            </xsl:call-template>
        </xsl:variable>
        <xsl:text>"</xsl:text>
          <xsl:value-of select="$escapedVal" />
        <xsl:text>"</xsl:text>
    </xsl:template>

    <xsl:template name="startJsonObject">
        <xsl:text>{</xsl:text>
    </xsl:template>

    <xsl:template name="endJsonObject">
        <xsl:text>}</xsl:text>
    </xsl:template>

    <xsl:template name="startJsonArray">
        <xsl:text>[</xsl:text>
    </xsl:template>

    <xsl:template name="endJsonArray">
        <xsl:text>]</xsl:text>
    </xsl:template>

</xsl:stylesheet>
