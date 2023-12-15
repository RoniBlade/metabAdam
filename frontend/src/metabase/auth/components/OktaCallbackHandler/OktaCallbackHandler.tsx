import { useEffect } from 'react';
import { useOktaAuth } from '@okta/okta-react';

console.log("Пользователь аутентифицирован: ");

const OktaCallbackHandler = () => {
  const { oktaAuth, authState } = useOktaAuth();

  useEffect(() => {
    if (!authState.isPending) {
      oktaAuth.handleAuthentication()
        .then(() => {
          if (authState.isAuthenticated && authState.user) {
            // Вы можете здесь использовать информацию о пользователе
            console.log("Пользователь аутентифицирован: ", authState.user);
          }
          // Перенаправить пользователя после успешной аутентификации
          window.location.replace('/');
        })
        .catch(error => {
          console.error('Ошибка обработки callback от Okta', error);
          // Здесь можно добавить перенаправление на страницу ошибки
          window.location.replace('/error-page');
        });
    }
  }, [oktaAuth, authState]);

  // Добавляем надпись "Привет" здесь
  return <div>Привет, обработка входа...</div>;
};

export default OktaCallbackHandler;
