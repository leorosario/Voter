package br.edu.ulbra.election.voter.service;

import br.edu.ulbra.election.voter.client.ElectionClientService;
import br.edu.ulbra.election.voter.exception.GenericOutputException;
import br.edu.ulbra.election.voter.input.v1.VoterInput;
import br.edu.ulbra.election.voter.model.Voter;
import br.edu.ulbra.election.voter.output.v1.GenericOutput;
import br.edu.ulbra.election.voter.output.v1.VoterOutput;
import br.edu.ulbra.election.voter.repository.VoterRepository;
import feign.FeignException;
import org.apache.commons.lang.StringUtils;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.lang.reflect.Type;
import java.util.List;

@Service
public class VoterService {

    private final VoterRepository voterRepository;

    private final ModelMapper modelMapper;

    private final PasswordEncoder passwordEncoder;

    private final ElectionClientService electionClientService;

    private static final String MESSAGE_INVALID_ID = "Invalid id";
    private static final String MESSAGE_VOTER_NOT_FOUND = "Voter not found";

    @Autowired
    public VoterService(VoterRepository voterRepository, ModelMapper modelMapper, PasswordEncoder passwordEncoder, ElectionClientService electionClientService) {
        this.voterRepository = voterRepository;
        this.modelMapper = modelMapper;
        this.passwordEncoder = passwordEncoder;
        this.electionClientService = electionClientService;
    }

    public List<VoterOutput> getAll() {
        Type voterOutputListType = new TypeToken<List<VoterOutput>>() {
        }.getType();
        return modelMapper.map(voterRepository.findAll(), voterOutputListType);
    }

    public VoterOutput create(VoterInput voterInput) {
        validateInput(voterInput, false, null);
        Voter voter = modelMapper.map(voterInput, Voter.class);
        voter.setPassword(passwordEncoder.encode(voter.getPassword()));
        voter = voterRepository.save(voter);
        return modelMapper.map(voter, VoterOutput.class);
    }

    public VoterOutput getById(Long voterId) {
        if (voterId == null) {
            throw new GenericOutputException(MESSAGE_INVALID_ID);
        }

        Voter voter = voterRepository.findById(voterId).orElse(null);
        if (voter == null) {
            throw new GenericOutputException(MESSAGE_VOTER_NOT_FOUND);
        }

        return modelMapper.map(voter, VoterOutput.class);
    }

    public VoterOutput update(Long voterId, VoterInput voterInput) {
        if (voterId == null) {
            throw new GenericOutputException(MESSAGE_INVALID_ID);
        }
        validateInput(voterInput, true, voterId);

        Voter voter = voterRepository.findById(voterId).orElse(null);
        if (voter == null) {
            throw new GenericOutputException(MESSAGE_VOTER_NOT_FOUND);
        }

        voter.setEmail(voterInput.getEmail());
        voter.setName(voterInput.getName());
        if (!StringUtils.isBlank(voterInput.getPassword())) {
            voter.setPassword(passwordEncoder.encode(voterInput.getPassword()));
        }
        voter = voterRepository.save(voter);
        return modelMapper.map(voter, VoterOutput.class);
    }

    public GenericOutput delete(Long voterId) {
        if (voterId == null) {
            throw new GenericOutputException(MESSAGE_INVALID_ID);
        }

        Voter voter = voterRepository.findById(voterId).orElse(null);
        if (voter == null) {
            throw new GenericOutputException(MESSAGE_VOTER_NOT_FOUND);
        }
        checkVote(voterId);
        voterRepository.delete(voter);

        return new GenericOutput("Voter deleted");
    }

    private void checkVote(Long id) {
        try {
            Boolean voto = electionClientService.getById(id);
            if (voto) {
                throw new GenericOutputException("This voter already has a vote");
            }
        } catch (FeignException e) {
            if (e.status() == 500) {
                throw new GenericOutputException("Invalid Election");
            }
        }
    }

    private void validateInput(VoterInput voterInput, boolean isUpdate, Long voterId) {
        Voter voterValidate;
        String[] palavras;
        if (StringUtils.isBlank(voterInput.getEmail())) {
            throw new GenericOutputException("Invalid email");
        }
        if (isUpdate) {
            voterValidate = voterRepository.findById(voterId).orElse(null);
            if (voterValidate != null) {
                if (!voterValidate.getEmail().equals(voterInput.getEmail())) {
                    if (voterRepository.findByEmail(voterInput.getEmail()) != null) {
                        throw new GenericOutputException("Invalid email");
                    }
                }
            }
        } else if (voterRepository.findByEmail(voterInput.getEmail()) != null) {
            throw new GenericOutputException("Invalid email");
        }
        if (StringUtils.isBlank(voterInput.getName())) {
            throw new GenericOutputException("Invalid name");
        }
        if (voterInput.getName().length() < 5) {
            throw new GenericOutputException("Invalid name. Name must be at least 5 characters.");
        }
        palavras = voterInput.getName().split(" ");
        if (palavras.length < 2) {
            throw new GenericOutputException("Invalid name. The name must have at least one last name.");
        }
        if (!StringUtils.isBlank(voterInput.getPassword())) {
            if (!voterInput.getPassword().equals(voterInput.getPasswordConfirm())) {
                throw new GenericOutputException("Passwords doesn't match");
            }
        } else {
            if (!isUpdate) {
                throw new GenericOutputException("Password doesn't match");
            }
        }
    }

}
