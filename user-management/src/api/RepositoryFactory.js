import ClientsRepository from './ClientsRepository';

const repositories = {
    clients: ClientsRepository
};

export const RepositoryFactory = {
  get: name => repositories[name]  
};