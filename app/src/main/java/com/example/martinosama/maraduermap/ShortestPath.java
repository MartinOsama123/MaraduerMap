package com.example.martinosama.maraduermap;

import android.util.Log;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;


public class ShortestPath {

    int INT_MAX = Integer.MAX_VALUE;
    ArrayList<ArrayList<Integer>> adj = new ArrayList<ArrayList<Integer>>(50);

    public ShortestPath(){
       for(int i = 0;i<30;i++)
           adj.add(new ArrayList<Integer>());
    }
    void add_edge(int src, int dest){
        adj.get(src).add(dest);
        adj.get(dest).add(src);
    }

    boolean BFS(  int src, int dest, int v,
             int pred[], int dist[]) {
        Queue<Integer> queue = new LinkedList<Integer>();
        boolean visited[] = new boolean[v];

        for (int i = 0; i < v; i++) {
            visited[i] = false;
            dist[i] = INT_MAX;
            pred[i] = -1;
        }

        visited[src] = true;
        dist[src] = 0;
        queue.add(src);

        while (!queue.isEmpty()) {
            int u = queue.peek();
            queue.remove();
            for (int i = 0; i < adj.get(u).size(); i++) {
                int temp = adj.get(u).get(i);
                if (visited[temp] == false) {
                    visited[temp] = true;
                    dist[temp] = dist[u] + 1;
                    pred[temp] = u;
                    queue.add(temp);
                    if (temp == dest)
                        return true;
                }
            }
        }

        return false;
    }

    public ArrayList<Integer> printShortestDistance( int s,
                               int dest, int v) {
        int pred[] = new int[v];
        int dist[] = new int[v];

        BFS(s, dest, v, pred, dist);

        ArrayList<Integer> path = new ArrayList<Integer>();
        int crawl = dest;
        path.add(crawl);
        while (pred[crawl] != -1) {
            path.add(pred[crawl]);
            crawl = pred[crawl];
        }

       Collections.reverse(path);
        return path;
    }
}
