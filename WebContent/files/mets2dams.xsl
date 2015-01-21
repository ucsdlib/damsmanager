<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
      xmlns:dams="http://library.ucsd.edu/ontology/dams#"
      xmlns:mads="http://www.loc.gov/mads/rdf/v1#"
      xmlns:mods="http://www.loc.gov/mods/v3"
      xmlns:mets="http://www.loc.gov/METS/"
      xmlns:owl="http://www.w3.org/2002/07/owl#"
      xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
      xmlns:xlink="http://www.w3.org/1999/xlink"
      xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
      xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#">
  <xsl:output method="xml" indent="yes"/>
  <xsl:variable name="madsNS">http://www.loc.gov/mads/rdf/v1#</xsl:variable>
  <xsl:variable name="damsid">http://library.ucsd.edu/ark:/20775/</xsl:variable>

  <!-- handle bare mods records -->
  <xsl:template match="/mods:mods">
    <dams:Object rdf:about="{generate-id()}">
      <xsl:apply-templates/>
      <xsl:call-template name="physical-description-note"/>
    </dams:Object>
  </xsl:template>

  <!-- handle modsCollection records as ProvenanceCollections -->
  <xsl:template match="mods:modsCollection">
    <dams:ProvenanceCollection rdf:about="{generate-id()}">
      <xsl:apply-templates/>
    </dams:ProvenanceCollection>
  </xsl:template>

  <!-- suppress desc md -->
  <xsl:template match="text()"/>
  <xsl:template match="mets:dmdSec"/>
  <xsl:template match="/mets:mets/mets:structMap[@TYPE='logical']/mets:div">
    <xsl:variable name="dmdid" select="@DMDID"/>
    <dams:Object rdf:about="{/mets:mets/@OBJID}">
      <xsl:call-template name="mods">
        <xsl:with-param name="dmdid" select="$dmdid"/>
      </xsl:call-template>
      <!-- desc md from dmdSec[@ID=$dmdid] -->
      <xsl:for-each select="//mets:dmdSec[@ID=$dmdid]/mets:mdWrap/mets:xmlData/mods:mods">
        <xsl:call-template name="physical-description-note"/>
      </xsl:for-each>
      <xsl:for-each select="mets:div">
        <xsl:call-template name="div"/>
      </xsl:for-each>
    </dams:Object>
  </xsl:template>
  <xsl:template name="div">
    <xsl:choose>
      <xsl:when test="@LABEL != ''">
        <xsl:variable name="dmdid" select="@DMDID"/>
        <dams:hasComponent>
          <dams:Component rdf:about="{/mets:mets/@OBJID}/CID">
            <dams:order><xsl:value-of select="@ORDER"/></dams:order>
            <xsl:choose>
              <xsl:when test="$dmdid != ''">
                <xsl:call-template name="mods">
                  <xsl:with-param name="dmdid" select="$dmdid"/>
                </xsl:call-template>
              </xsl:when>
              <xsl:when test="@LABEL != ''">
                <dams:title>
                  <mads:Title>
                    <mads:authoritativeLabel><xsl:value-of select="@LABEL"/></mads:authoritativeLabel>
                    <mads:elementList rdf:parseType="Collection">
                      <mads:MainTitleElement>
                        <mads:elementValue><xsl:value-of select="@LABEL"/></mads:elementValue>
                      </mads:MainTitleElement>
                    </mads:elementList>
                  </mads:Title>
                </dams:title>
              </xsl:when>
            </xsl:choose>
            <xsl:for-each select="mets:div">
              <xsl:call-template name="div"/>
            </xsl:for-each>
          </dams:Component>
        </dams:hasComponent>
      </xsl:when>
      <xsl:when test="@TYPE='page' and mets:fptr">
        <xsl:call-template name="file"/>
      </xsl:when>
      <xsl:otherwise>
        <!-- ??? -->
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  <xsl:template name="file">
    <xsl:variable name="fid" select="mets:fptr/@FILEID"/>
    <xsl:for-each select="//mets:file[@ID=$fid]">
      <xsl:variable name="fileAbout">
        <xsl:choose>
          <xsl:when test="mets:FLocat/@LOCTYPE='URL'
                  and starts-with(mets:FLocat/@xlink:href,'http://library.ucsd.edu/ark:/')">
            <xsl:value-of select="mets:FLocat/@xlink:href"/>
          </xsl:when>
          <xsl:otherwise><xsl:value-of select="/mets:mets/@OBJID"/>/FID</xsl:otherwise>
        </xsl:choose>
      </xsl:variable>
      <dams:hasFile>
        <dams:File rdf:about="{$fileAbout}">
          <dams:use>
            <xsl:choose>
              <xsl:when test="@USE = 'Audio-Master'">audio-source</xsl:when>
              <xsl:when test="@USE = 'Audio-Master-Edited'">audio-alternate</xsl:when>
              <xsl:when test="@USE = 'Audio-Service'">audio-service</xsl:when>
              <xsl:when test="@USE = 'Application-PDF'">document-service</xsl:when>
              <xsl:when test="@USE = 'Image-Master'">image-source</xsl:when>
              <xsl:when test="@USE = 'Image-Master-Edited'">image-alternate</xsl:when>
              <xsl:when test="@USE = 'Image-Service'">image-service</xsl:when>
              <xsl:when test="@USE = 'Image-Service-HighRes'">image-large</xsl:when>
              <xsl:when test="@USE = 'Image-Service-MedRes'">image-preview</xsl:when>
              <xsl:when test="@USE = 'Image-Service-LowRes'">image-thumbnail</xsl:when>
              <xsl:when test="@USE = 'Image-Thumbnail'">image-icon</xsl:when>
              <xsl:when test="@USE = 'Text-Service'">document-service</xsl:when>
              <xsl:when test="@USE = 'Text-Master'">document-source</xsl:when>
              <xsl:when test="@USE = 'Text-Data'">data-source</xsl:when>
              <xsl:when test="@USE = 'Video-Master'">video-source</xsl:when>
              <xsl:when test="@USE = 'Video-Master-Edited'">video-alternate</xsl:when>
              <xsl:when test="@USE = 'Video-Service'">video-service</xsl:when>
            </xsl:choose>
          </dams:use>
          <dams:sourceFileName>
            <xsl:value-of select="mets:FLocat/@xlink:href"/>
          </dams:sourceFileName>
        </dams:File>
      </dams:hasFile>
    </xsl:for-each>
  </xsl:template>
  <xsl:template name="mods">
    <xsl:param name="dmdid"/>
    <!-- desc md from dmdSec[@ID=$dmdid] -->
    <xsl:for-each select="//mets:dmdSec[@ID=$dmdid]/mets:mdWrap/mets:xmlData/mods:mods">
      <xsl:apply-templates/>
    </xsl:for-each>
  </xsl:template>
  <xsl:template name="physical-description-note">
    <xsl:if test="mods:physicalDescription/mods:extent
               or mods:physicalDescription/mods:note[@displayLabel='General Physical Description note']
               or mods:physicalDescription/mods:note[@displayLabel='Physical Facet note']
               or mods:note[@displayLabel='extent']
               or mods:note[@diplayLabel='dimensions']">
      <dams:note>
        <dams:Note>
          <dams:type>physical description</dams:type>
          <rdf:value>
            <xsl:call-template name="physical-description-element">
              <xsl:with-param name="value" select="mods:physicalDescription/mods:note[@displayLabel='General Physical Description note']"/>
            </xsl:call-template>
            <xsl:call-template name="physical-description-element">
              <xsl:with-param name="value" select="mods:physicalDescription/mods:note[@displayLabel='Physical Facet note']"/>
            </xsl:call-template>
            <xsl:call-template name="physical-description-element">
              <xsl:with-param name="value" select="mods:note[@displayLabel='extent']"/>
            </xsl:call-template>
            <xsl:call-template name="physical-description-element">
              <xsl:with-param name="value" select="mods:physicalDescription/mods:extent"/>
            </xsl:call-template>
            <xsl:call-template name="physical-description-element">
              <xsl:with-param name="value" select="mods:note[@displayLabel='dimensions']"/>
              <xsl:with-param name="last">true</xsl:with-param>
            </xsl:call-template>
          </rdf:value>
        </dams:Note>
      </dams:note>
    </xsl:if>
  </xsl:template>
  <xsl:template name="physical-description-element">
    <xsl:param name="value"/>
    <xsl:param name="last"/>
    <xsl:if test="$value != ''">
      <xsl:value-of disable-output-escaping="yes" select="$value"/>
      <xsl:if test="$last != 'true'"><xsl:text>; </xsl:text></xsl:if>
    </xsl:if>
  </xsl:template>
  <xsl:template match="mods:titleInfo">
    <dams:title>
      <mads:Title>
        <mads:authoritativeLabel>
          <xsl:value-of select="mods:nonSort"/>
          <xsl:value-of select="mods:title"/>
          <xsl:for-each select="mods:subTitle">
            <xsl:text>, </xsl:text><xsl:value-of select="."/>
          </xsl:for-each>
          <xsl:for-each select="mods:partNumber">
            <xsl:text>, </xsl:text><xsl:value-of select="."/>
          </xsl:for-each>
        </mads:authoritativeLabel>
        <xsl:for-each select="mods:subTitle">
          <dams:subtitle><xsl:value-of select="."/></dams:subtitle>
        </xsl:for-each>
        <xsl:for-each select="mods:partNumber">
          <dams:partNumber><xsl:value-of select="."/></dams:partNumber>
        </xsl:for-each>
        <mads:elementList rdf:parseType="Collection">
          <xsl:apply-templates/>
        </mads:elementList>
      </mads:Title>
    </dams:title>
  </xsl:template>
  <xsl:template match="mods:titleInfo/mods:title">
    <mads:MainTitleElement>
      <mads:elementValue><xsl:value-of select="."/></mads:elementValue>
    </mads:MainTitleElement>
  </xsl:template>
  <xsl:template match="mods:titleInfo/mods:nonSort">
    <mads:NonSortElement>
      <mads:elementValue><xsl:value-of select="."/></mads:elementValue>
    </mads:NonSortElement>
  </xsl:template>
  <xsl:template match="mods:titleInfo/mods:subTitle">
    <mads:SubTitleElement>
      <mads:elementValue><xsl:value-of select="."/></mads:elementValue>
    </mads:SubTitleElement>
  </xsl:template>
  <xsl:template match="mods:titleInfo/mods:partNumber">
    <mads:PartNumberElement>
      <mads:elementValue><xsl:value-of select="."/></mads:elementValue>
    </mads:PartNumberElement>
  </xsl:template>
  <xsl:template match="mods:mods/mods:language">
    <dams:language>
      <mads:Language rdf:about="{generate-id()}">
        <xsl:for-each select="mods:languageTerm">
          <xsl:choose>
            <xsl:when test="@type='code'">
              <mads:code><xsl:value-of select="."/></mads:code>
              <xsl:call-template name="authority">
                <xsl:with-param name="auth" select="@authority"/>
                <xsl:with-param name="code" select="."/>
              </xsl:call-template>
            </xsl:when>
            <xsl:when test="@type='text'">
              <mads:authoritativeLabel>
                <xsl:value-of select="."/>
              </mads:authoritativeLabel>
              <mads:elementList rdf:parseType="Collection">
                <mads:LanguageElement>
                  <mads:elementValue>
                    <xsl:value-of select="."/>
                  </mads:elementValue>
                </mads:LanguageElement>
              </mads:elementList>
            </xsl:when>
          </xsl:choose>
        </xsl:for-each>
      </mads:Language>
    </dams:language>
  </xsl:template>
  <xsl:template match="mods:mods/mods:typeOfResource">
    <dams:typeOfResource>
      <xsl:choose>
        <xsl:when test="text() != ''">
          <xsl:value-of select="."/>
        </xsl:when>
        <xsl:when test="@collection = 'yes'">collection</xsl:when>
      </xsl:choose>
    </dams:typeOfResource>
  </xsl:template>
  <xsl:template match="mods:mods/mods:relatedItem">
    <!-- only generate RelatedResource if there is a URL -->
    <xsl:if test="mods:location/mods:url">
      <dams:relatedResource>
        <dams:RelatedResource>
          <xsl:if test="mods:titleInfo/mods:title">
            <dams:description>
              <xsl:value-of select="mods:titleInfo/mods:title"/>
              <xsl:if test="mods:name">
                <xsl:text> by </xsl:text>
                <xsl:for-each select="mods:name/mods:namePart">
                  <xsl:if test="position() &gt; 1"><xsl:text> </xsl:text></xsl:if>
                  <xsl:value-of select="."/>
                </xsl:for-each>
              </xsl:if>
            </dams:description>
          </xsl:if>
          <xsl:if test="mods:location/mods:url">
            <xsl:if test="mods:location/mods:url/@note">
              <dams:description>
                <xsl:value-of select="mods:location/mods:url/@note"/>
              </dams:description>
              <dams:uri><xsl:value-of select="mods:location/mods:url"/></dams:uri>
            </xsl:if>
          </xsl:if>
        </dams:RelatedResource>
      </dams:relatedResource>
    </xsl:if>
    <xsl:for-each select="mods:note">
      <dams:note>
        <dams:Note>
          <xsl:choose>
            <xsl:when test="@displayLabel = 'Existence and Location of Copies note'">
            </xsl:when>
            <xsl:when test="@displayLabel = 'Existence and Location of Originals note'">
              <dams:type>location of originals</dams:type>
            </xsl:when>
            <xsl:otherwise>
              <xsl:if test="@displaylabel != ''">
                <dams:displaylabel><xsl:value-of select="@displaylabel"/></dams:displaylabel>
              </xsl:if>
              <xsl:if test="@type != ''">
                <dams:type><xsl:value-of select="@type"/></dams:type>
              </xsl:if>
            </xsl:otherwise>
          </xsl:choose>
          <rdf:value><xsl:value-of select="."/></rdf:value>
        </dams:Note>
      </dams:note>
    </xsl:for-each>
  </xsl:template>
  <xsl:template match="mods:physicalDescription/mods:extent">
    <!-- see physical-description-note -->
  </xsl:template>
  <xsl:template match="mods:physicalDescription/mods:note">
    <xsl:choose>
      <xsl:when test="@displayLabel = 'Material Specific Details note'">
        <dams:note>
          <dams:Note>
            <dams:type>material details</dams:type>
            <rdf:value><xsl:value-of select="."/></rdf:value>
          </dams:Note>
        </dams:note>
      </xsl:when>
      <xsl:when test="@displayLabel = 'Physical Characteristics and Technical Requirements note'">
        <dams:note>
          <dams:Note>
            <dams:type>technical requirements</dams:type>
            <rdf:value><xsl:value-of select="."/></rdf:value>
          </dams:Note>
        </dams:note>
      </xsl:when>
      <xsl:when test="@displayLabel = 'General Physical Description note'">
        <!-- see physical-description-note -->
      </xsl:when>
      <xsl:when test="@displayLabel = 'Physical Facet note'">
        <!-- see physical-description-note -->
      </xsl:when>
      <xsl:otherwise>
        <dams:note>
          <dams:Note>
            <xsl:call-template name="generic-note"/>
          </dams:Note>
        </dams:note>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  <xsl:template match="mods:mods/mods:abstract">
    <dams:note>
      <dams:Note>
        <xsl:choose>
          <xsl:when test="@displayLabel = 'Scope and Contents note'">
            <dams:type>scope and content</dams:type>
          </xsl:when>
          <xsl:when test="@displayLabel = 'Abstract' or @displayLabel = 'inscription'">
            <dams:type>description</dams:type>
          </xsl:when>
          <xsl:otherwise>
            <dams:displayLabel>
              <xsl:choose>
                <xsl:when test="@displayLabel != ''">
                  <xsl:value-of select="@displayLabel"/>
                </xsl:when>
                <xsl:otherwise>Abstract</xsl:otherwise>
              </xsl:choose>
            </dams:displayLabel>
            <dams:type>description</dams:type>
          </xsl:otherwise>
        </xsl:choose>
        <rdf:value><xsl:value-of select="."/></rdf:value>
      </dams:Note>
    </dams:note>
  </xsl:template>
  <xsl:template match="mods:mods/mods:note">
    <xsl:if test="text() != ''">
      <dams:note>
        <dams:Note>
          <xsl:choose>
            <!-- display label maps to blank type -->
            <xsl:when test="@displayLabel = 'General note'">
              <rdf:value><xsl:value-of select="."/></rdf:value>
            </xsl:when>
            <xsl:when test="@displayLabel = 'Related Archival Materials note'">
              <rdf:value><xsl:value-of select="."/></rdf:value>
            </xsl:when>

            <!-- display label maps to custom type -->
            <xsl:when test="@displayLabel = 'Biographical/Historical note'">
              <dams:type>biography</dams:type>
              <rdf:value><xsl:value-of select="."/></rdf:value>
            </xsl:when>
            <xsl:when test="@displayLabel = 'General Physical Description note'">
              <!-- see physical-description-note -->
            </xsl:when>
            <xsl:when test="@displayLabel = 'Immediate Source of Acquisition note'">
              <dams:type>custodial history</dams:type>
              <rdf:value><xsl:value-of select="."/></rdf:value>
            </xsl:when>
            <xsl:when test="@displayLabel = 'Preferred Citation note'">
              <dams:type>preferred citation</dams:type>
              <rdf:value><xsl:value-of select="."/></rdf:value>
            </xsl:when>

            <!-- display label maps to type and display label -->
            <xsl:when test="@displayLabel = 'identifier:ARK'">
              <dams:type>identifier</dams:type>
              <dams:displayLabel>ARK</dams:displayLabel>
              <rdf:value><xsl:value-of select="."/></rdf:value>
            </xsl:when>
            <xsl:when test="@displayLabel = 'identifier:basket'">
              <dams:type>identifier</dams:type>
              <dams:displayLabel>basket</dams:displayLabel>
              <rdf:value><xsl:value-of select="."/></rdf:value>
            </xsl:when>
            <xsl:when test="@displayLabel = 'identifier:collection number'">
              <dams:type>identifier</dams:type>
              <dams:displayLabel>collection number</dams:displayLabel>
              <rdf:value><xsl:value-of select="."/></rdf:value>
            </xsl:when>
            <xsl:when test="@displayLabel = 'identifier:call number'">
              <dams:type>identifier</dams:type>
              <dams:displayLabel>call number</dams:displayLabel>
              <rdf:value><xsl:value-of select="."/></rdf:value>
            </xsl:when>
            <xsl:when test="@displayLabel = 'identifier:DOI'">
              <dams:type>identifier</dams:type>
              <dams:displayLabel>DOI</dams:displayLabel>
              <rdf:value><xsl:value-of select="."/></rdf:value>
            </xsl:when>
            <xsl:when test="@displayLabel = 'identifier:EDM'">
              <dams:type>identifier</dams:type>
              <dams:displayLabel>EDM</dams:displayLabel>
              <rdf:value><xsl:value-of select="."/></rdf:value>
            </xsl:when>
            <xsl:when test="@displayLabel = 'identifier:filename'">
              <dams:type>identifier</dams:type>
              <dams:displayLabel>filename</dams:displayLabel>
              <rdf:value><xsl:value-of select="."/></rdf:value>
            </xsl:when>
            <xsl:when test="@displayLabel = 'identifier:IGSN number'">
              <dams:type>identifier</dams:type>
              <dams:displayLabel>IGSN number</dams:displayLabel>
              <rdf:value><xsl:value-of select="."/></rdf:value>
            </xsl:when>
            <xsl:when test="@displayLabel = 'identifier:isbn'">
              <dams:type>identifier</dams:type>
              <dams:displayLabel>ISBN</dams:displayLabel>
              <rdf:value><xsl:value-of select="."/></rdf:value>
            </xsl:when>
            <xsl:when test="@displayLabel = 'identifier:lccn'">
              <dams:type>identifier</dams:type>
              <dams:displayLabel>LCCN</dams:displayLabel>
              <rdf:value><xsl:value-of select="."/></rdf:value>
            </xsl:when>
            <xsl:when test="@displayLabel = 'identifier:local'">
              <dams:type>identifier</dams:type>
              <dams:displayLabel>local</dams:displayLabel>
              <rdf:value><xsl:value-of select="."/></rdf:value>
            </xsl:when>
            <xsl:when test="@displayLabel = 'identifier:negative'">
              <dams:type>identifier</dams:type>
              <dams:displayLabel>negative</dams:displayLabel>
              <rdf:value><xsl:value-of select="."/></rdf:value>
            </xsl:when>
            <xsl:when test="@displayLabel = 'identifier:OCLC number'">
              <dams:type>identifier</dams:type>
              <dams:displayLabel>OCLC number</dams:displayLabel>
              <rdf:value><xsl:value-of select="."/></rdf:value>
            </xsl:when>
            <xsl:when test="@displayLabel = 'identifier:registration number'">
              <dams:type>identifier</dams:type>
              <dams:displayLabel>registration number</dams:displayLabel>
              <rdf:value><xsl:value-of select="."/></rdf:value>
            </xsl:when>
            <xsl:when test="@displayLabel = 'identifier:roger record'">
              <dams:type>identifier</dams:type>
              <dams:displayLabel>roger record</dams:displayLabel>
              <rdf:value><xsl:value-of select="."/></rdf:value>
            </xsl:when>
            <xsl:when test="@displayLabel = 'identifier:sample number'">
              <dams:type>identifier</dams:type>
              <dams:displayLabel>sample number</dams:displayLabel>
              <rdf:value><xsl:value-of select="."/></rdf:value>
            </xsl:when>
            <xsl:when test="@displayLabel = 'identifier:sequence'">
              <dams:type>identifier</dams:type>
              <dams:displayLabel>sequence</dams:displayLabel>
              <rdf:value><xsl:value-of select="."/></rdf:value>
            </xsl:when>
            <xsl:when test="@displayLabel = 'material details:storage method'">
              <dams:type>material details</dams:type>
              <dams:displayLabel>storage method</dams:displayLabel>
              <rdf:value><xsl:value-of select="."/></rdf:value>
            </xsl:when>
            <xsl:when test="@displayLabel = 'material details:water depth'">
              <dams:type>material details</dams:type>
              <dams:displayLabel>water depth</dams:displayLabel>
              <rdf:value><xsl:value-of select="."/></rdf:value>
            </xsl:when>

            <!-- displayLabel maps to type -->
            <xsl:when test="@displayLabel = 'arrangement'
                         or @displayLabel = 'bibliography'
                         or @displayLabel = 'classification'
                         or @displayLabel = 'credits'
                         or @displayLabel = 'digital origin'
                         or @displayLabel = 'edition'
                         or @displayLabel = 'inscription'
                         or @displayLabel = 'local attribution'
                         or @displayLabel = 'performers'
                         or @displayLabel = 'publication'
                         or @displayLabel = 'series'
                         or @displayLabel = 'statement of responsibility'
                         or @displayLabel = 'table of contents'
                         or @displayLabel = 'thesis'
                         or @displayLabel = 'venue'">
              <dams:type><xsl:value-of select="@displayLabel"/></dams:type>
              <rdf:value><xsl:value-of select="."/></rdf:value>
            </xsl:when>
            <xsl:when test="@displayLabel = 'dimensions' or @displayLabel = 'extent'">
              <!-- see physical-description-note -->
            </xsl:when>
            <xsl:otherwise>
              <xsl:call-template name="generic-note"/>
            </xsl:otherwise>
          </xsl:choose>
        </dams:Note>
      </dams:note>
    </xsl:if>
  </xsl:template>
  <xsl:template name="generic-note">
    <!-- default note if type/label do not match vocabulary -->
    <rdf:value>
      <xsl:if test="@displayLabel != ''">
        <xsl:value-of select="@displayLabel"/><xsl:text>: </xsl:text>
      </xsl:if>
      <xsl:value-of select="."/>
    </rdf:value>
  </xsl:template>
  <xsl:template match="mods:accessCondition">
    <xsl:choose>
      <xsl:when test="@displayLabel = 'Rights'">
        <dams:copyright>
          <dams:Copyright rdf:about="{generate-id()}">
            <dams:copyrightStatus>Under copyright</dams:copyrightStatus>
            <dams:copyrightJurisdiction>us</dams:copyrightJurisdiction>
            <dams:copyrightNote><xsl:value-of select="."/></dams:copyrightNote>
            <xsl:for-each select="//mods:accessCondition[@displayLabel='Access']">
              <dams:copyrightPurposeNote>
                <xsl:value-of select="."/>
              </dams:copyrightPurposeNote>
            </xsl:for-each>
          </dams:Copyright>
        </dams:copyright>
      </xsl:when>
      <xsl:when test="@displayLabel = 'License'">
        <dams:license>
          <dams:License rdf:about="{generate-id()}">
            <dams:licenseNote><xsl:value-of select="."/></dams:licenseNote>
          </dams:License>
        </dams:license>
      </xsl:when>
      <xsl:when test="@displayLabel = 'Legal Status note'">
        <dams:note>
          <dams:Note>
            <rdf:value><xsl:value-of select="."/></rdf:value>
          </dams:Note>
        </dams:note>
      </xsl:when>
      <xsl:when test="@displayLabel = 'Conditions Governing Access note'"></xsl:when>
      <xsl:when test="@displayLabel = 'Conditions Governing Use note'"></xsl:when>
    </xsl:choose>
  </xsl:template>
  <xsl:template match="mods:identifier">
    <xsl:if test="@invalid != 'yes'">
      <dams:note>
        <dams:Note>
          <dams:type>identifier</dams:type>
          <dams:displayLabel><xsl:value-of select="@type"/></dams:displayLabel>
          <xsl:choose>
            <xsl:when test="@displayLabel = 'ARK'
                         or @displayLabel = 'basket'
                         or @displayLabel = 'collection number'
                         or @displayLabel = 'call number'
                         or @displayLabel = 'DOI'
                         or @displayLabel = 'EDM'
                         or @displayLabel = 'filename'
                         or @displayLabel = 'IGSN number'
                         or @displayLabel = 'isbn'
                         or @displayLabel = 'lccn'
                         or @displayLabel = 'local'
                         or @displayLabel = 'negative'
                         or @displayLabel = 'OCLC number'
                         or @displayLabel = 'registration number'
                         or @displayLabel = 'roger record'
                         or @displayLabel = 'sample number'
                         or @displayLabel = 'sequence'">
              <rdf:value><xsl:value-of select="."/></rdf:value>
            </xsl:when>
            <xsl:otherwise>
              <rdf:value>
                <xsl:text>identifier:</xsl:text>
                <xsl:value-of select="@displayLabel"/>
                <xsl:text>: </xsl:text>
                <xsl:value-of select="."/>
              </rdf:value>
            </xsl:otherwise>
          </xsl:choose>
        </dams:Note>
      </dams:note>
    </xsl:if>
  </xsl:template>
  <xsl:template match="mods:originInfo">
    <xsl:if test="mods:dateCreated|mods:dateIssued|mods:dateOther">
      <dams:date>
        <dams:Date>
          <xsl:for-each select="mods:dateCreated|mods:dateIssued|mods:dateOther">
            <xsl:if test="@type != ''">
              <dams:type><xsl:value-of select="@type"/></dams:type>
            </xsl:if>
            <xsl:choose>
              <xsl:when test="@point='start'">
                <dams:beginDate><xsl:value-of select="."/></dams:beginDate>
              </xsl:when>
              <xsl:when test="@point='end'">
                <dams:endDate><xsl:value-of select="."/></dams:endDate>
              </xsl:when>
              <xsl:otherwise>
                <rdf:value><xsl:value-of select="."/></rdf:value>
              </xsl:otherwise>
            </xsl:choose>
          </xsl:for-each>
        </dams:Date>
      </dams:date>
    </xsl:if>
    <xsl:if test="mods:publisher|mods:place">
      <dams:note>
        <dams:Note>
          <dams:type>publication</dams:type>
          <rdf:value>
            <xsl:if test="mods:place/mods:placeTerm[@type !='code' or not(@type)] != ''">
              <xsl:value-of select="mods:place/mods:placeTerm[@type!='code' or not(@type)]"/>
            </xsl:if>
            <xsl:if test="mods:publisher != '' and mods:place/mods:placeTerm[@type!='code' or not(@type)] != ''">, </xsl:if>
            <xsl:if test="mods:publisher != ''">
              <xsl:value-of select="mods:publisher"/>
            </xsl:if>
          </rdf:value>
        </dams:Note>
      </dams:note>
    </xsl:if>
  </xsl:template>
  <xsl:template match="mods:mods/mods:name">
    <dams:relationship>
      <dams:Relationship>
        <dams:role>
          <mads:Authority rdf:about="{generate-id()}">
            <xsl:choose>
              <xsl:when test="mods:role">
                <xsl:for-each select="mods:role/mods:roleTerm[@type='code']">
                  <mads:code><xsl:value-of select="."/></mads:code>
                  <xsl:call-template name="authority">
                    <xsl:with-param name="auth" select="@authority"/>
                    <xsl:with-param name="code" select="."/>
                  </xsl:call-template>
                </xsl:for-each>
                <xsl:for-each select="mods:role/mods:roleTerm[@type='text']">
                  <mads:authoritativeLabel>
                    <xsl:value-of select="."/>
                  </mads:authoritativeLabel>
                </xsl:for-each>
              </xsl:when>
              <xsl:otherwise>
                <mads:code>cre</mads:code>
                <rdf:value>Creator</rdf:value>
              </xsl:otherwise>
            </xsl:choose>
          </mads:Authority>
        </dams:role>
        <xsl:variable name="predicateName">
          <xsl:choose>
            <xsl:when test="@type='personal'">personalName</xsl:when>
            <xsl:when test="@type='corporate'">corporateName</xsl:when>
            <xsl:otherwise>name</xsl:otherwise>
          </xsl:choose>
        </xsl:variable>
        <xsl:element name="dams:{$predicateName}">
          <xsl:call-template name="name"/>
        </xsl:element>
      </dams:Relationship>
    </dams:relationship>
  </xsl:template>
  <xsl:template name="authority">
    <xsl:param name="auth"/>
    <xsl:param name="code"/>
    <xsl:param name="uri"/>
    <xsl:choose>
      <xsl:when test="$uri != ''">
        <mads:hasExactExternalAuthority rdf:resource="{$uri}"/>
      </xsl:when>
      <xsl:when test="$auth = 'iso639-2b' and $code != ''">
        <mads:hasExactExternalAuthority rdf:resource="http://id.loc.gov/vocabulary/iso639-2/{$code}"/>
      </xsl:when>
      <xsl:when test="$auth = 'marcrelator' and code != ''">
        <mads:hasExactExternalAuthority rdf:resource="http://id.loc.gov/vocabulary/relators/{$code}"/>
      </xsl:when>
    </xsl:choose>
    <xsl:if test="$auth != ''">
      <mads:isMemberOfMADSScheme>
        <mads:MADSScheme rdf:about="{generate-id()}">
          <mads:code><xsl:value-of select="$auth"/></mads:code>
          <xsl:choose>
            <xsl:when test="$auth = 'iso639-2b'">
              <mads:hasExactExternalAuthority rdf:resource="http://id.loc.gov/vocabulary/iso639-2"/>
              <rdfs:label>ISO 639 Language Codes</rdfs:label>
            </xsl:when>
            <xsl:when test="$auth = 'lcsh'">
              <mads:hasExactExternalAuthority rdf:resource="http://id.loc.gov/authorities/subjects"/>
              <rdfs:label>Library of Congress Subject Headings</rdfs:label>
            </xsl:when>
            <xsl:when test="$auth = 'marcrelator'">
              <mads:hasExactExternalAuthority rdf:resource="http://id.loc.gov/vocabulary/relators"/>
              <rdfs:label>MARC Relator Codes</rdfs:label>
            </xsl:when>
            <xsl:when test="$auth = 'naf'">
              <mads:hasExactExternalAuthority rdf:resource="http://id.loc.gov/authorities/names"/>
              <rdfs:label>Library of Congress Name Authority File</rdfs:label>
            </xsl:when>
            <xsl:when test="$auth = 'rbgenr'">
              <mads:hasExactExternalAuthority rdf:resource="http://www.rbms.info/committees/bibliographic_standards/controlled_vocabularies/genre/"/>
              <rdfs:label>RBMS Genre Terms</rdfs:label>
            </xsl:when>
            <xsl:when test="$auth = 'local'">
              <mads:hasExactExternalAuthority rdf:resource="http://library.ucsd.edu/ontology/dams/vocabulary"/>
              <rdfs:label>Local</rdfs:label>
            </xsl:when>
          </xsl:choose>
        </mads:MADSScheme>
      </mads:isMemberOfMADSScheme>
    </xsl:if>
  </xsl:template>
  <xsl:template name="name" match="mods:subject/mods:name">
    <xsl:variable name="elementName">
      <xsl:choose>
        <xsl:when test="@type='personal'">PersonalName</xsl:when>
        <xsl:when test="@type='corporate'">CorporateName</xsl:when>
        <xsl:otherwise>Name</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:element name="mads:{$elementName}">
      <xsl:attribute name="rdf:about">
        <xsl:value-of select="generate-id()"/>
      </xsl:attribute>
      <mads:authoritativeLabel>
        <xsl:choose>
          <xsl:when test="mods:displayForm != ''">
            <xsl:value-of select="mods:displayForm"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:for-each select="mods:namePart">
              <xsl:if test="position() &gt; 1">, </xsl:if>
              <xsl:value-of select="."/>
            </xsl:for-each>
          </xsl:otherwise>
        </xsl:choose>
      </mads:authoritativeLabel>
      <xsl:call-template name="authority">
        <xsl:with-param name="auth" select="@authority"/>
      </xsl:call-template>
      <xsl:if test="mods:description != ''">
        <mads:hasExactExternalAuthority rdf:resource="{normalize-space(mods:description)}"/>
      </xsl:if>
      <mads:elementList rdf:parseType="Collection">
        <xsl:choose>
          <xsl:when test="count(mods:namePart) = 1
                and $elementName = 'PersonalName'">
            <mads:FullNameElement>
              <mads:elementValue>
                <xsl:value-of select="mods:namePart"/>
              </mads:elementValue>
            </mads:FullNameElement>
          </xsl:when>
          <xsl:when test="count(mods:namePart) = 1">
            <mads:NameElement>
              <mads:elementValue>
                <xsl:value-of select="mods:namePart"/>
              </mads:elementValue>
            </mads:NameElement>
          </xsl:when>
          <xsl:when test="count(mods:namePart) &gt; 1">
            <xsl:for-each select="mods:namePart">
              <xsl:variable name="subelement">
                <xsl:choose>
                  <xsl:when test="@type='family'">FamilyNameElement</xsl:when>
                  <xsl:when test="@type='given'">GivenNameElement</xsl:when>
                  <xsl:when test="@type='date'">DateNameElement</xsl:when>
                  <xsl:otherwise>NameElement</xsl:otherwise>
                </xsl:choose>
              </xsl:variable>
              <xsl:element name="mads:{$subelement}" namespace="{$madsNS}">
                <mads:elementValue>
                  <xsl:value-of select="."/>
                </mads:elementValue>
              </xsl:element>
            </xsl:for-each>
          </xsl:when>
        </xsl:choose>
      </mads:elementList>
    </xsl:element>
  </xsl:template>
  <xsl:template match="mods:mods/mods:subject">
    <xsl:choose>
      <xsl:when test="count(*) &gt; 1">
        <dams:complexSubject>
          <mads:ComplexSubject rdf:about="{generate-id()}">
            <xsl:call-template name="authority">
              <xsl:with-param name="auth" select="@authority"/>
            </xsl:call-template>
            <mads:authoritativeLabel>
              <xsl:for-each select="*">
                <xsl:if test="position() &gt; 1">--</xsl:if>
                <xsl:choose>
                  <xsl:when test="local-name() = 'name'">
                    <xsl:for-each select="mods:namePart">
                      <xsl:if test="position() &gt; 1">, </xsl:if>
                      <xsl:choose>
                        <xsl:when test="mods:displayForm">
                          <xsl:value-of select="mods:displayForm"/>
                        </xsl:when>
                        <xsl:otherwise><xsl:value-of select="."/></xsl:otherwise>
                      </xsl:choose>
                    </xsl:for-each>
                  </xsl:when>
                  <xsl:otherwise>
                    <xsl:value-of select="."/>
                  </xsl:otherwise>
                </xsl:choose>
              </xsl:for-each>
            </mads:authoritativeLabel>
            <mads:componentList rdf:parseType="Collection">
              <xsl:apply-templates/>
            </mads:componentList>
          </mads:ComplexSubject>
        </dams:complexSubject>
      </xsl:when>
      <xsl:when test="mods:name[@type='personal']">
        <dams:personalName><xsl:apply-templates/></dams:personalName>
      </xsl:when>
      <xsl:when test="mods:name[@type='corporate']">
        <dams:corporateName><xsl:apply-templates/></dams:corporateName>
      </xsl:when>
      <xsl:when test="mods:name">
        <dams:name><xsl:apply-templates/></dams:name>
      </xsl:when>
      <xsl:when test="mods:genre">
        <dams:genreForm><xsl:apply-templates/></dams:genreForm>
      </xsl:when>
      <xsl:when test="mods:geographic">
        <dams:geographic><xsl:apply-templates/></dams:geographic>
      </xsl:when>
      <xsl:when test="mods:occupation">
        <dams:occupation><xsl:apply-templates/></dams:occupation>
      </xsl:when>
      <xsl:when test="mods:temporal">
        <dams:temporal><xsl:apply-templates/></dams:temporal>
      </xsl:when>
      <xsl:when test="mods:topic">
        <dams:topic><xsl:apply-templates/></dams:topic>
      </xsl:when>
      <xsl:when test="mods:hierarchicalGeographic">
        <dams:geographic>
          <mads:Geographic>
            <xsl:variable name="label">
              <xsl:for-each select="mods:hierarchicalGeographic/*">
                <xsl:if test="position() &gt; 1">--</xsl:if>
                <xsl:value-of select="."/>
              </xsl:for-each>
            </xsl:variable>
            <mads:authoritativeLabel>
              <xsl:value-of select="$label"/>
            </mads:authoritativeLabel>
            <mads:elementList rdf:parseType="Collection">
              <mads:GeographicElement>
                <mads:elementValue>
                  <xsl:value-of select="$label"/>
                </mads:elementValue>
              </mads:GeographicElement>
            </mads:elementList>
          </mads:Geographic>
        </dams:geographic>
      </xsl:when>
      <xsl:otherwise>
        <xsl:apply-templates/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  <xsl:template match="mods:mods/mods:genre">
    <dams:genreForm><xsl:call-template name="simplesubject"/></dams:genreForm>
  </xsl:template>
  <xsl:template name="simplesubject" match="mods:genre|mods:geographic|mods:occupation|mods:temporal|mods:topic">
    <xsl:variable name="elemName">
      <xsl:choose>
        <xsl:when test="local-name() = 'genre'">GenreForm</xsl:when>
        <xsl:when test="local-name() = 'geographic'">Geographic</xsl:when>
        <xsl:when test="local-name() = 'occupation'">Occupation</xsl:when>
        <xsl:when test="local-name() = 'temporal'">Temporal</xsl:when>
        <xsl:when test="local-name() = 'topic'">Topic</xsl:when>
        <xsl:otherwise>ZZZ<xsl:value-of select="name()"/></xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:element name="mads:{$elemName}">
      <xsl:attribute name="rdf:about">
        <xsl:value-of select="generate-id()"/>
      </xsl:attribute>
      <xsl:call-template name="authority">
        <xsl:with-param name="auth" select="../@authority"/>
      </xsl:call-template>
      <mads:authoritativeLabel>
        <xsl:value-of select="."/>
      </mads:authoritativeLabel>
      <mads:elementList rdf:parseType="Collection">
        <xsl:element name="mads:{$elemName}Element">
          <mads:elementValue>
            <xsl:value-of select="."/>
          </mads:elementValue>
        </xsl:element>
      </mads:elementList>
    </xsl:element>
  </xsl:template>
</xsl:stylesheet>
