const submit = document.querySelector("#submit")
submit.addEventListener("keydown", e => {
  // console.log(e)   //uncomment this if you want to see key codes and names etc...
  clickPress(e)
})

function clickPress(e) {
  if (e.keyCode == 13) {
    obtainJson()
  }
}

function showDissectedResults(data) {
  let response = document.getElementById("response")
  //TODO: Finish making response for result of creatingUser here.
}

function obtainJson() {
  let username = document.getElementById("username").value
  let email = document.getElementById("email").value
  let password = document.getElementById("password").value
  fetch('/bus/createUser', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      uname: username,
      email: email,
      password: password
    })
  }).then(res => {
    return res.json()
  }).then(data => {
    showDissectedResults(data)
  })
}
