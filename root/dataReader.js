

async function returnData(url) {
    async function load(Url) {
        const request = await fetch(Url);
        const json = request.json();
        return json;
    }

    let data = await load(url);
    return data;
}

async function sendRequest(url) {
    await fetch(url);
}

function displayPlanets(data) {
    data.forEach(el => {
        let body = document.getElementById(el.id);
        body.style.top = el.posY + "px";
        body.style.left = el.posX + "px";
        let center = document.getElementById("center-" + el.id);
        center.style.top = el.posY + "px";
        center.style.left = el.posX + "px";

        let bodyWindow = document.getElementById("window-" + el.id);
        bodyWindow.querySelector(".window-inner-text").innerHTML = generateWindowInnerText(el);

        if(focusOn === el.id){
            focusOnElement(body);
        }

        if (generateOrbit2) {
            let bodyCenter = document.createElement("div");
            bodyCenter.className = "celestialBodyCenter";
            bodyCenter.style.top = el.posY + "px";
            bodyCenter.style.left = el.posX + "px";
            celestialMap.appendChild(bodyCenter);
        }
    })
}


function displayStartUpData(data) {
    data.forEach(generateCelestialObject)
}

async function displayBodyFromDatabase(name) {
    let bodyData = await returnData("http://"+myIP+":8080/get-one?name=" + name);
    generateCelestialObject(bodyData);
}

function generateCelestialObject(data) {
    generateBodyCenter(data);
    generateBody(data);
    generateWindow(data);
    generateMyListElement(data);
    generatePrimaryBodiesListElement(data);
}

function displayBodiesListFromApi(data) {
    data.bodies.forEach(el => {
        apiBodyList.push([el.englishName, el.id]);
    })
    apiBodyList.forEach(el => {
        let addThisName = true;
        celestialBodies.forEach(body => {
            if (body.textContent === el[0]) {
                addThisName = false;
            }
        })
        if (addThisName) {
            generateApiListElement(el[0])
        }
    })
}


function generateWindowInnerText(data) {
    return "    <b>name:</b>   " + data.name + "<br/>" +
        "    <b>mass:</b>   " + data.massValue + "e" + data.massExponent + "<br/>" +
        "    <b>radius:</b> " + data.radius + "<br/>" +
        "    <b>color:</b>  " + data.color + "<br/>" +
        "    <b>velocity:</b>   " + parseFloat(Math.sqrt(data.velX * data.velX + data.velY * data.velY)).toFixed(6) + "<br/>" +
        "    <b>posX:</b>  " + data.posX + "<br/>" +
        "    <b>posY:</b>  " + data.posY + "<br/>";


}
