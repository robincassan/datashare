describe('DataShare - Parcours complet', () => {
  const email = `test-${Date.now()}@example.com`;
  const password = 'Test1234!';
  const fileName = 'test-e2e.txt';

  before(() => {
    cy.writeFile(`cypress/fixtures/${fileName}`, 'Contenu de test E2E');
  });

  it('US03 - Création de compte', () => {
    cy.visit('/register');
    cy.get('h2').should('contain', 'Inscription');

    cy.get('input[name="email"]').type(email);
    cy.get('input[name="password"]').type(password);
    cy.get('input[name="confirmPassword"]').type(password);
    cy.get('button[type="submit"]').click();

    cy.url({ timeout: 5000 }).should('include', '/login');
  });

  it('US03 - Échec création avec email existant', () => {
    cy.visit('/register');
    cy.get('input[name="email"]').type(email);
    cy.get('input[name="password"]').type(password);
    cy.get('input[name="confirmPassword"]').type(password);
    cy.get('button[type="submit"]').click();

    cy.get('.error', { timeout: 5000 }).should('be.visible');
  });

  it('US04 - Connexion', () => {
    cy.visit('/login');
    cy.get('h2').should('contain', 'Connexion');

    cy.get('input[name="email"]').type(email);
    cy.get('input[name="password"]').type(password);
    cy.get('button[type="submit"]').click();

    cy.url({ timeout: 5000 }).should('include', '/dashboard');
    cy.get('h2').should('contain', 'Mes fichiers');
  });

  it('US04 - Échec connexion mauvais mot de passe', () => {
    cy.visit('/login');
    cy.get('input[name="email"]').type(email);
    cy.get('input[name="password"]').type('wrongpassword');
    cy.get('button[type="submit"]').click();

    cy.get('.error', { timeout: 5000 }).should('be.visible');
  });

  it('US01 - Upload de fichier', () => {
    cy.visit('/login');
    cy.get('input[name="email"]').type(email);
    cy.get('input[name="password"]').type(password);
    cy.get('button[type="submit"]').click();
    cy.url({ timeout: 5000 }).should('include', '/dashboard');

    cy.get('input[type="file"]').selectFile(`cypress/fixtures/${fileName}`);
    cy.get('input[name="password"]').type('filepass');
    cy.get('button').contains('Uploader').click();

    cy.get('.success', { timeout: 5000 }).should('contain', 'Fichier uploadé');
    cy.contains(fileName).should('be.visible');
  });

  it('US05 - Consultation historique fichiers', () => {
    cy.visit('/login');
    cy.get('input[name="email"]').type(email);
    cy.get('input[name="password"]').type(password);
    cy.get('button[type="submit"]').click();
    cy.url({ timeout: 5000 }).should('include', '/dashboard');

    cy.contains(fileName).should('be.visible');
    cy.get('.file-card').should('have.length.at.least', 1);
  });

  it('US06 - Suppression de fichier', () => {
    cy.visit('/login');
    cy.get('input[name="email"]').type(email);
    cy.get('input[name="password"]').type(password);
    cy.get('button[type="submit"]').click();
    cy.url({ timeout: 5000 }).should('include', '/dashboard');

    cy.get('.delete').first().click();
    cy.contains('Aucun fichier', { timeout: 5000 }).should('be.visible');
  });
});
