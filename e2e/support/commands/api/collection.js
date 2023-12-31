Cypress.Commands.add(
  "createCollection",
  ({
    name,
    description = null,
    parent_id = null,
    authority_level = null,
  } = {}) => {
    cy.log(`Create a collection: ${name}`);
    return cy.request("POST", "/api/collection", {
      name,
      description,
      parent_id,
      authority_level,
    });
  },
);

Cypress.Commands.add("archiveCollection", id => {
  cy.log(`Archiving a collection with id: ${id}`);
  return cy.request("PUT", `/api/collection/${id}`, {
    archived: true,
  });
});
