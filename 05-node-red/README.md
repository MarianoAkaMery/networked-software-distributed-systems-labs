# Evaluation lab - Node-RED

## Group number: 36

## Group members

- Salvatore Mariano Librici 
- Rong Huang
- Mohammadali Amiri

## Description of message flows

The Telegram Receiver node collects user messages and forwards them to the Parse Command function, which identifies the command sent (QUERY, TRACK, or REPORT) and extracts the city, country, and user ID.

For QUERY, the location is passed to the OpenWeather node, which returns the current temperature. A chatbot-message node formats the answer and sends it to the user.

For TRACK, the selected location is stored in a flow-level dictionary indexed by the user’s Telegram ID. This allows each user to maintain their own list of tracked locations. A confirmation message is returned.

For REPORT, all saved locations for the user are retrieved and split so each one can be processed individually by the OpenWeather node. Each result is then collected and merged into a single multiline message with a Join node, and finally sent back to the user through the Telegram Sender node.

## Extensions 

node-red-contrib-chatbot

node-red-node-openweathermap

## Configuration

The flow requires a Telegram bot token and an OpenWeather API key configured locally in Node-RED.
Do not commit real tokens or API keys to the repository.

