const IDPS_TO_SHOW = ["idir_aad", "phsa", "bcprovider_aad", "fnha_aad"];

function hideIdentityProviders(allIdentityProviders) {
    allIdentityProviders.forEach(idpAlias => {
        if(IDPS_TO_SHOW.indexOf(idpAlias) === -1){
            document.getElementById('zocial-' + idpAlias).style.display = 'none';
        }
    })
}

// When the user clicks on the button, open the modal
function showInstructionsModal() {
    var modal = document.getElementById("instructions-modal");
    modal.style.display = "block";
};

function hideInstructionsModal() {
    var modal = document.getElementById("instructions-modal");
    modal.style.display = "none";
};

// When the user clicks anywhere outside of the modal, close it
window.onclick = function(event) {
    var modal = document.getElementById("instructions-modal");
    if (event.target == modal) {
        modal.style.display = "none";
    }
};
