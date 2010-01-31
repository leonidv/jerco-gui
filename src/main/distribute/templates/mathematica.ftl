g = {	
	<#list nodes as node>
		<#list node.linkedNodes as ln> ${node.id} -> ${ln.id}, </#list>
	</#list>		 
}