package com.company;

import javafx.util.Pair;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class Main {


    public class Node{
        double degree;
        LinkedList<Pair<Date, String>> adjacentNodes;
    }


    public void executeAction (String inputPath, String outputPath) {
        // read the json file and make it as json object in java
        try{
            // Open the file
            FileInputStream fstream = new FileInputStream(inputPath);
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            BufferedWriter bw = new BufferedWriter(new FileWriter(outputPath));

            String strLine;

            // initialize the Venmo graph
            // Key --> the name of the person in the node (String)
            // Value --> the linked list of the pair (the person connected to the node & the time when payment has occurred)
            HashMap<String, Node> graph = new HashMap<String, Node>();

            // initialize the most recent payment date and the least payment date allowed
            Date leastPaymentDateAllowed = new Date();
            Date mostRecentPaymentDate = new Date(); // maximum timestamp
            Calendar cal = Calendar.getInstance();
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH);

            // a flag variable that is used to initialize the date objects with the first data entry's createdTime
            // if the date objects are initialized with the first data entry's createdTime, the flag variable
            // will be false
            Boolean dateInitializerFlag = true;
            
            DecimalFormat format = new DecimalFormat("0.00");

            //Read File Line By Line and build a graph
            while ((strLine = br.readLine()) != null)   {
                // read each line, and make it into jsonobject
                JSONObject jsonObject = new JSONObject(strLine);

                // get the data of each line
                String actor = jsonObject.getString("actor");
                String target = jsonObject.getString("target");
                Date createdTime = dateFormat.parse(jsonObject.getString("created_time"));

                // if any of the three fields is missing, ignore the data
                if (actor == null || actor == ""){continue;}
                if (target == null || target == ""){continue;}
                if (createdTime == null){continue;}

                // initialize the most recent and least payment dates if this is the first data entry
                if (dateInitializerFlag){
                    mostRecentPaymentDate = createdTime;
                    leastPaymentDateAllowed = new Date(mostRecentPaymentDate.getTime() - 60000);
                    dateInitializerFlag = false;
                    // make the flag false so that they are not re-initialized anymore
                }

                // if the time of the new data entry is more than 60 seconds from the maximum timestamp processed,
                // or in other words, if createdTime is before the least payment time allowed,
                // ignore the data
                if (createdTime.before(leastPaymentDateAllowed)){continue;}

                // update the most recent payment date if the new data has more recent date than what is in
                // the variable (mostRecentPaymentDate)
                if (mostRecentPaymentDate.before(createdTime)){
                    mostRecentPaymentDate = createdTime;
                }

                // if the difference between the maximum timestamp and the value of the leastPaymentDateAllowed
                // is more than 60 seconds (60000),
                if (mostRecentPaymentDate.getTime() - leastPaymentDateAllowed.getTime() > 60000){
                    // the time difference is always in milliseconds
                    // remove the data whose createdTime is earlier than the least recent payment date allowed
                    // update the least payment date
                    leastPaymentDateAllowed = new Date(mostRecentPaymentDate.getTime() - 60000);
                    // remove the older ones whose createdTime is ealier than the leastPaymentDateAllowed
                    Remove(graph, leastPaymentDateAllowed);
                }

                // add the new data entry to the graph
                Add(graph, actor, target, createdTime);

                // calculate the rolling median
                double median = getMedian(graph);

                // write to the output file
                bw.write(format.format(median) + "\n");
            }

            //Close the input and output streams
            br.close();
            bw.close();
        }catch(Exception ex){
            ex.printStackTrace();
        }

    }

    void Remove(HashMap<String, Node> graph, Date leastPaymentDateLimit){

        for (String key : graph.keySet()) {
            LinkedList<Pair<Date, String>> tmpAdjacentNodes = (LinkedList<Pair<Date,String>>) graph.get(key).adjacentNodes.clone();
            for (Pair<Date, String> pairobj : graph.get(key).adjacentNodes) {
                if (pairobj.getKey().before(leastPaymentDateLimit)) {
                    // remove the adjacent node
                    if (graph.get(key).adjacentNodes.contains(pairobj)) {
                        tmpAdjacentNodes.remove(pairobj);
                        graph.get(key).degree--;
                    }
                }
            }

            graph.get(key).adjacentNodes = tmpAdjacentNodes;
        }
    }

    void Add(HashMap<String, Node> graph, String actor, String target, Date createdTime){
        // check if the graph contains actor
        if (graph.containsKey(actor)){
            Pair<Date, String> obj = new Pair(createdTime, target);
            graph.get(actor).adjacentNodes.add(obj);
            graph.get(actor).degree++;
        }else{
            LinkedList<Pair<Date, String>> newAdjacentNodes = new LinkedList<Pair<Date, String>>();
            Pair<Date, String> obj = new Pair(createdTime, target);
            newAdjacentNodes.add(obj);
            Node node = new Node();
            node.degree++;
            node.adjacentNodes = newAdjacentNodes;
            graph.put(actor, node);
        }
        // check if the same for target
        if (graph.containsKey(target)){
            Pair<Date, String> obj = new Pair(createdTime, actor);
            graph.get(target).adjacentNodes.add(obj);
            graph.get(target).degree++;
        }else{
            LinkedList<Pair<Date, String>> newAdjacentNodes = new LinkedList<Pair<Date, String>>();
            Pair<Date, String> obj = new Pair(createdTime, actor);
            newAdjacentNodes.add(obj);
            Node node = new Node();
            node.degree++;
            node.adjacentNodes = newAdjacentNodes;
            graph.put(target, node);
        }
    }

    double getMedian(HashMap<String, Node> graph){

        Collection<Node> values = graph.values();
        // reason for choosing Array over Linked
        ArrayList<Double> degrees = new ArrayList<Double>();
        values.forEach(a -> {degrees.add(a.degree);});

        Object[] degreesArray = degrees.toArray();
        Arrays.sort(degreesArray);

        double median;
        if (degreesArray.length % 2 == 0) {
            median = Math.floor(((degreesArray[degreesArray.length / 2] + degreesArray[degreesArray.length / 2 - 1]) / 2)*100)/100;
        }else {
            median = Math.floor(degreesArray[degreesArray.length / 2]*100)/100;
        }

        return median;
    }


    public static void main(String[] args) {
        // write your code here
        String inputPath = "";
        String outputPath = "";

        String workingBaseDirectory = System.getProperty("user.dir");

        if (args != null && args.length >= 2){
            // read the paths of both input and output
            args[0] = args[0].substring(1);
            args[1] = args[1].substring(1);
            inputPath = workingBaseDirectory.concat(args[0]);
            outputPath = workingBaseDirectory.concat(args[1]);
        }else{
            inputPath = workingBaseDirectory.concat("/venmo_input/venmo-trans.txt");
            outputPath = workingBaseDirectory.concat("/venmo_output/output.txt");
        }

        Main main = new Main();

        // execute the action
        main.executeAction(inputPath, outputPath);

    }
}
