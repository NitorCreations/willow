<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:template match="@time[.='undefined']">
    <xsl:attribute name="time">
      <xsl:value-of select="'0.0'"/>
    </xsl:attribute>
  </xsl:template>
  <xsl:template match="@*|node()">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
   </xsl:copy>
  </xsl:template>
</xsl:stylesheet>
