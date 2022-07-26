module "payara-client" {
	source = "../../../../modules/payara-client"
	claim_name  = "gis_role"
	client_id   = "GIS"
	base_url    = "https://gis.hlth.gov.bc.ca/gis"
	description = "Guaranteed Income Support"
	valid_redirect_uris = [
		"https://sts.healthbc.org/adfs/ls/*",
		"https://logon7.gov.bc.ca/clp-cgi/logoff.cgi*",
		"https://gis.hlth.gov.bc.ca/*",
	]
	roles = {	
        "GISUSER" = {
            "name" = "GISUSER"
            "description" = ""
        },
	}
}
