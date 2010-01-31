digraph G {
	node [
		fontsize = "8"
		shape = "circle"
		width = "0.3"
		height= "0.3"
		margin= "0.01"
	]

	edge [
		arrowhead = "none"
	]
	
	<#list nodes as node>
		${node.id} [ 
			label="${node.id}"	<#if node.inBound> 
			fillcolor = "red" 
			style = "filled"
			</#if>
		]	
	</#list>
	
	<#list nodes as node>
		${node.id} -> {<#list node.linkedNodes as ln> ${ln.id} </#list>}
	</#list>
		 
}