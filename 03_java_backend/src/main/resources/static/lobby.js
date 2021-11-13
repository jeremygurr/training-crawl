// React.js lobby page. This page will let people create accounts, login and reset password.
// It also shows a table of current top players from around the world.
// When players hit 'continue' or 'start new adventure', they will be taken to the main
// page with canvas based webgame inside it.
// source for page: https://www.taniarascia.com/getting-started-with-react/
// babel is used to allow es6+ functions in older browsers.

let reactScript = document.createElement('script');
let reactDomScript = document.createElement('script');
let babelScript = document.createElement('script');
jQueryScript.setAttribute('src','https://unpkg.com/react@^16/umd/react.production.min.js');
reactDomScript.setAttribute('src','https://unpkg.com/react-dom@16.13.0/umd/react-dom.production.min.js');
babelScript.setAttribute('src','https://unpkg.com/babel-standalone@6.26.0/babel.js');
document.head.appendChild(reactScript, reactDomScript, babelScript);

class App extends React.Component {
  render() {
      return (
          <h1>Hello World</h1>
      );
}

ReactDOM.render(<App />, document.getElementById('root'))


