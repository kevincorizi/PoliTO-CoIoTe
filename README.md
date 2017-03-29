# PoliTO-CoIoTe
Randomic heuristic greedy solution to one-to-many assignment problem in a IoT urban scenario.

# What is CoIoTe?
It is a new service being developed to connect IoT objects to Internet without requiring the deployment of a new ad-hoc wireless network.
Smart City IoT objects need connection to the Internet in order to communicate measures and receive configurations.

# Solution proposed?
Hotspot WiFi networks created on-the-fly by the smartphones of a certain carrier passing by IoT objects (dumpsters in this specific case) needing to send data over the Internet.

# Assignment
Optimize a system capable of assigning missions to mobile users: users have to ben sent near to dumpsters withing the city in order to give them connectivity for some seconds. Such dumpsters need connectivity so they can send their waste filling level to the waste company server in order to better schedule the emptying of the dumpsters.

# Challenges
Mobile users will not participate unless rewarded: need for a tradeoff between level of connectivity and cost of engaging users.

# Goal
Cover all the dumpsters minimizing the costs.

# Setup
The city is divided into cells, there is a certain number of users in each cell and users can move across cells. It is possible to have different types of users according to some policy. A user can move in the city in a certain moment of the day. A certain type of user in a certain cell of the city in a certain time of the day has a cost to move to any other cell.

# Constraints
- All dumpsters must be visited
- Cannot use more users than there actually are
- Algorithm must terminate in 5 seconds
- Solutions must have a maximum optimality gap of 2%

# Results
Our algorithm guarantees an average optimality gap of 0.1% in 5 seconds for all proposed input instances.
Our team was one of the two best teams in the class Optimization Methods and Algorithm for this assignment.
