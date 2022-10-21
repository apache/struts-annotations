<table class="tag-reference">
    <tr>
        <td colspan="6"><h4>Dynamic Attributes Allowed:</h4> ${tag.allowDynamicAttributes?string}</td>
    </tr>
    <tr>
        <td colspan="6"><hr/></td>
    </tr>
    <tr>
        <th class="tag-header"><h4>Name</h4></th>
        <th class="tag-header"><h4>Required</h4></th>
        <th class="tag-header"><h4>Default</h4></th>
        <th class="tag-header"><h4>Evaluated</h4></th>
        <th class="tag-header"><h4>Type</h4></th>
        <th class="tag-header"><h4>Description</h4></th>
    </tr>
<#list tag.attributes as att>
    <tr>
        <td class="tag-attribute">${att.name}</td>
        <td class="tag-attribute"><#if att.required><strong>true</strong><#else>false</#if></td>
        <td class="tag-attribute">${att.defaultValue}</td>
        <td class="tag-attribute">${att.rtexprvalue?string}</td>
        <td class="tag-attribute">${att.type}</td>
        <td class="tag-attribute">${att.description}</td>
    </tr>
</#list>
</table>
