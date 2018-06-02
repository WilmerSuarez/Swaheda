'use strict'

const functions = require('firebase-functions');
const admin = require ('firebase-admin');
admin.initializeApp(functions.config().firebase);

exports.sendNotification = functions.database.ref('/Notifications/{receiver_id}/{notification_id}')
	.onWrite(event => {
		const receiver_id = event.params.receiver_id;
		const notification_id = event.params.notification_id;
		console.log('We have a notification to send to: ', receiver_id);

		if(!event.data.val()) {
			return console.log('A notification has been deleted from the database', notification_id);
		}

		const deviceToken = admin.database().ref(`/Users/${receiver_id}/device_token`).once('value');
		return deviceToken.then(result => {
			const token_id = result.val();
			const payload = {
				notification: {
					title: "Friend Request", 
					body: "You have a received a new friend request!",
					icon: "default"
				}
			};

			return admin.messaging().sendToDevice(token_id, paload)
				.then(response => {
					return console.log('This was the notification feature.');
				});
		});
	});

