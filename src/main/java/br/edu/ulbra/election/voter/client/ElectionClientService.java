package br.edu.ulbra.election.voter.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Service
public class ElectionClientService {

    private final ElectionClient electionClient;

    @Autowired
    public ElectionClientService(ElectionClient electionClient){
        this.electionClient = electionClient;
    }

    public Boolean getById(Long id){
        return this.electionClient.getById(id);
    }

    @FeignClient(value="election-service", url="${url.election-service}")
    private interface ElectionClient {

        @GetMapping("/v1/vote/{voterId}")
        Boolean getById(@PathVariable(name = "voterId") Long voterId);
    }
}
