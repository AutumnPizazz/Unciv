import DefaultTheme from 'vitepress/theme'
import CopyButton from './CopyButton.vue'
import './custom.css'

export default {
  extends: DefaultTheme,
  enhanceApp({ app }) {
    app.component('CopyButton', CopyButton)
  },
}
