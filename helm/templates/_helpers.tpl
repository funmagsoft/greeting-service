{{/* 
Return the name of the chart 
*/}}
{{- define "greeting-service.name" -}}
{{- .Chart.Name -}}
{{- end -}}

{{/* 
Return the fullname, np. chartname + release name
*/}}
{{- define "greeting-service.fullname" -}}
{{- printf "%s" .Release.Name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
