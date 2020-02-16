<table width="100%">
    <tr>
        <td colspan="6"><h4>Dynamic Attributes Allowed:</h4> ${tag.allowDynamicAttributes?string}</td>
    </tr>
    <tr>
        <td colspan="6">&nbsp;</td>
    </tr>
    <tr>
        <th align="left" valign="top"><h4>Name</h4></th>
        <th align="left" valign="top"><h4>Required</h4></th>
        <th align="left" valign="top"><h4>Default</h4></th>
        <th align="left" valign="top"><h4>Evaluated</h4></th>
        <th align="left" valign="top"><h4>Type</h4></th>
        <th align="left" valign="top"><h4>Description</h4></th>
    </tr>
    <#list tag.attributes as att>
        <tr>
            <td align="left" valign="top">${att.name}</td>
            <td align="left" valign="top"><#if att.required><strong>true</strong><#else>false</#if></td>
            <td align="left" valign="top">${att.defaultValue}</td>
            <td align="left" valign="top">${att.rtexprvalue?string}</td>
            <td align="left" valign="top">${att.type}</td>
            <td align="left" valign="top">${att.description}</td>
        </tr>
    </#list>
</table>
